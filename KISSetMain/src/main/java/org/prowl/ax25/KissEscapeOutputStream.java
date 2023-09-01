package org.prowl.ax25;
/*
 * Copyright (C) 2011-2019 Andrew Pavlin, KA2DDO
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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This filtering OutputStream adds the KISS protocol escape sequences for the
 * body of a KISS frame. It also calculates G8BPQ's CRC for the frame if needed
 * for MKISS operations.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public class KissEscapeOutputStream extends OutputStream {


    /**
     * Byte value for end-of-frame flag byte in KISS protocol.
     */
    public static final int FEND = 0xC0;
    /**
     * Byte value of prefix for escaped byte value (use of protocol byte value in frame body).
     */
    public static final int FESC = 0xDB;
    /**
     * Escaped value for literal FEND character.
     */
    public static final int TFEND = 0xDC;
    /**
     * Escaped value for literal FESC character.
     */
    public static final int TFESC = 0xDD;
    private static final Log LOG = LogFactory.getLog("KissEscapeOutputStream");
    private final OutputStream os;
    private int byteCount = 0;
    private byte g8bpqCrc = 0;

    /**
     * Create a KissEscapeOutputStream wrapped around an OutputStream.
     *
     * @param os OutputStream to receive KISS-encoded frames
     */
    public KissEscapeOutputStream(OutputStream os) {
        this.os = os;
    }


    /**
     * Get the number of bytes passed through this stream (counting escape codes injected by the stream).
     *
     * @return byte count
     */
    public int getByteCount() {
        return byteCount;
    }

    /**
     * Reset the statistics fields for this stream.
     */
    public void resetByteCount() {
        this.byteCount = 0;
        this.g8bpqCrc = 0;
    }

    /**
     * Write one byte to the output stream.
     *
     * @param c byte value to encode
     * @throws IOException if wrapped stream throws an IOException
     */
    public void write(int c) throws IOException {
        int b = c & 0xFF;
        if (b == FEND) {
            os.write(FESC);
            os.write(TFEND);
            byteCount += 2;
        } else if (b == FESC) {
            os.write(FESC);
            os.write(TFESC);
            byteCount += 2;
        } else {
            os.write(b);
            byteCount++;
        }

        g8bpqCrc ^= (byte) b;
    }

    public void write(byte[] body) {
        for (byte b : body) {
            try {
                write(b);
            } catch (IOException e) {
                LOG.error("Error writing to stream", e);
            }
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        // len == 0 condition implicitly handled by loop bounds
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }

    public void flush() throws IOException {
        os.flush();
    }

    public void close() throws IOException {
        os.close();
    }

    /**
     * Write one byte to the output stream.
     *
     * @param b byte value to encode
     * @throws IOException if wrapped stream throws an IOException
     */
    public void writeRaw(int b) throws IOException {
        os.write(b);
        byteCount++;
    }


    /**
     * Get the G8BPQ CRC value for the last sent KISS frame.
     *
     * @return one-byte CRC as used by G8BPQ
     */
    public byte getG8bpqCrc() {
        return g8bpqCrc;
    }

    /**
     * States of a KISS frame decoder.
     */
    public enum RcvState {
        /**
         * KISS decoder has not received the first FEND byte since (re-)initialization.
         */
        IDLE,
        /**
         * KISS decoder has received an FEND byte and is waiting for more body bytes.
         */
        IN_FRAME,
        /**
         * KISS decoder has received an FESC byte and is waiting for the TFEND or TFESC byte to indicate
         * which byte code was escaped.
         */
        IN_ESC
    }
}