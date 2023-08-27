package org.prowl.ax25;
/*
 * Copyright (C) 2011-2022 Andrew Pavlin, KA2DDO
 * This file is part of YAAC (Yet Another APRS Client).
 *
 *  YAAC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  YAAC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and GNU Lesser General Public License along with YAAC.  If not,
 *  see <http://www.gnu.org/licenses/>.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Converts a standard Java output stream of bytes into AX.25 connected-mode I-frames.
 *
 * @author Andrew Pavlin, KA2DDO
 */
class AX25OutputStream extends OutputStream {

    private static final Log LOG = LogFactory.getLog("AX25OutputStream");

    private final byte[] buf; // maximum body length of AX.25 frame (like ax.25 paclen)
    private final ConnState connState;
    private int bufIdx = 0;

    AX25OutputStream(ConnState connState, int pacLen) {
        this.connState = connState;
        connState.transmitWindow = new AX25Frame[connState.connType == ConnState.ConnType.MOD128 ? 128 : 8];
        buf = new byte[pacLen];
    }

    /**
     * Writes the specified byte to this output stream. The general
     * contract for <code>write</code> is that one byte is written
     * to the output stream. The byte to be written is the eight
     * low-order bits of the argument <code>b</code>. The 24
     * high-order bits of <code>b</code> are ignored.
     * <p>
     * Subclasses of <code>OutputStream</code> must provide an
     * implementation for this method.
     *
     * @param b the <code>byte</code>.
     * @throws IOException if an I/O error occurs. In particular,
     *                     an <code>IOException</code> may be thrown if the
     *                     output stream has been closed.
     */
    public synchronized void write(int b) throws IOException {
        b = b & 0xFF;
        buf[bufIdx++] = (byte) b;
        if (bufIdx >= buf.length) {
            flush();
        }
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this output stream.
     * The general contract for <code>write(b, off, len)</code> is that
     * some of the bytes in the array <code>b</code> are written to the
     * output stream in order; element <code>b[off]</code> is the first
     * byte written and <code>b[off+len-1]</code> is the last byte written
     * by this operation.
     * <p>
     * The <code>write</code> method of <code>OutputStream</code> calls
     * the write method of one argument on each of the bytes to be
     * written out. Subclasses are encouraged to override this method and
     * provide a more efficient implementation.
     * <p>
     * If <code>b</code> is <code>null</code>, a
     * <code>NullPointerException</code> is thrown.
     * <p>
     * If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs. In particular,
     *                     an <code>IOException</code> is thrown if the output
     *                     stream is closed.
     */
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return;
        } else if (len < 0 || off < 0 || len + off > b.length) {
            throw new IndexOutOfBoundsException();
        }

        // Simply forward to out byte processing to keep things simple.
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }

    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out. The general contract of <code>flush</code> is
     * that calling it is an indication that, if any bytes previously
     * written have been buffered by the implementation of the output
     * stream, such bytes should immediately be written to their
     * intended destination.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized void flush() throws IOException {
        synchronized (connState) {

            if (bufIdx > 0) {
                if (!connState.isOpen()) {
                    throw new EOFException("AX.25 connection closed");
                }
                AX25Frame f = new AX25Frame();

                // fill in header
                if (connState.stack.isLocalDest(connState.src)) {
                    f.sender = connState.src.dup();
                    f.dest = connState.dst.dup();
                    if (connState.via != null) {
                        AX25Callsign[] digis = new AX25Callsign[connState.via.length];
                        for (int i = 0; i < connState.via.length; i++) {
                            digis[i] = connState.via[i].dup();
                            digis[i].h_c = false;
                        }
                        f.digipeaters = digis;
                    }
                } else {
                    f.sender = connState.dst.dup();
                    f.dest = connState.src.dup();
                    f.digipeaters = connState.stack.reverseDigipeaters(connState.via);
                }
                f.ctl = AX25Frame.FRAMETYPE_I;
                f.mod128 = (ConnState.ConnType.MOD128 == connState.connType);
                f.setPid(AX25Frame.PID_NOLVL3);
                f.setCmd(true);
                f.body = new byte[bufIdx];
                System.arraycopy(buf, 0, f.body, 0, bufIdx);

                // ijh this code is just weird (and didn't work)
                // submit new frame to destination (blocking if window buffer is full)
                ;
//                do {
//                    nextVS = connState.vs;
//
////                    if (connState.transmitWindow[nextVS] != null) {
////                        v
////                        while (connState.transmitWindow[nextVS] != null && nextVS != connState.vs) {
////                            nextVS = (nextVS + 1) % (f.mod128 ? 128 : 8);
////                        }
////                    }
//
//                    // AX.25 Spec says we can transmit N(R)-1 frames before waiting for an ACK
//                    //  int nextNextVS = (nextVS + 1 + (7 - connState.stack.maxFrames)) % (f.mod128 ? 128 : 8);
//                    if (connState.transmitWindow[nextVS] == null) {
//                        break;
//                    }
//                    // window buffer is completely full, wait until there is room
//                    synchronized (connState) {
//                        try {
//                            wait(1000L);
//                        } catch (InterruptedException e) {
//                            // ignore
//                        }
//                    }
//                    if (!connState.isOpen()) {
//                        throw new EOFException("AX.25 connection closed");
//                    }
//                } while (true);

                // A more sensible way to count the number of frames in the transmit window
                // keeping to the N(R)-1 rule
                int nextVS = connState.modSentFrameIndex;
                int counter = 60000;
                while (counter + 2 >= (f.mod128 ? 128 : 8)) {
                    counter = 0;
                    for (AX25Frame frame : connState.transmitWindow) {
                        if (frame != null) {
                            counter++;
                        }
                    }
                    if (counter + 2 < (f.mod128 ? 128 : 8) && counter < connState.stack.maxFrames) {
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Throwable e) {
                    }
                }

                connState.transmitWindow[nextVS] = f;

                if (nextVS == connState.modSentFrameIndex) {
                    connState.modSentFrameIndex = (connState.modSentFrameIndex + 1) % (f.mod128 ? 128 : 8);
                }
                if (!connState.xmtToRemoteBlocked) {
                    if (connState.connector != null) {
                        f.setNS(nextVS);
                        f.setNR(connState.modReceivedFrameIndex);
                        LOG.debug("sending I frame " + f.sender + "->" + f.dest + " NS=" + f.getNS() + " NR=" + f.getNR() + " #=" + f.body.length);
                        //connState.stack.getTransmitting().queue(f);
                        connState.connector.sendFrame(f);
                        connState.setResendableFrame(f, connState.stack.getTransmitting().getRetransmitCount()); // Make sure we resend it if we have no ack
                    } else {
                        throw new NullPointerException("no TransmittingConnector to send data through");
                    }
                }

                bufIdx = 0;
            }
        }
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. The general contract of <code>close</code>
     * is that it closes the output stream. A closed stream cannot perform
     * output operations and cannot be reopened.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        flush();
    }

    /**
     * Produce a String representation of the object.
     *
     * @return String describing the stream and its state
     */
    @Override
    public String toString() {
        return "AX25OutputStream[" + connState.paramString() + ']';
    }
}
