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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Convert a sequence of incoming ordered I frames from an AX.25 connection into a
 * Java standard input byte stream.
 *
 * @author Andrew Pavlin, KA2DDO
 */
class AX25InputStream extends InputStream {
    private final ConnState connState;
    List<AX25Frame> frameQueue = new LinkedList<AX25Frame>();
    transient AX25Frame currentFrame = null;
    int frameBodyIndex = 0;

    /**
     * Create an AX25InputStream based on the specified AX.25 connection state object
     *
     * @param connState ConnState object describing the outstanding AX.25 connection
     */
    AX25InputStream(ConnState connState) {
        this.connState = connState;
    }

    synchronized void add(AX25Frame f) {
        frameQueue.add(f);
        notifyAll();
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if an I/O error occurs.
     */
    public int read() throws IOException {
        synchronized (this) {
            while (currentFrame == null && connState.connType != ConnState.ConnType.CLOSED && frameQueue.size() == 0) {
                frameBodyIndex = 0;
                try {
                    wait(10000L);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            if (ConnState.ConnType.CLOSED == connState.connType) {
                return -1;
            }
            if (currentFrame == null) {
                currentFrame = frameQueue.remove(0);
            }
        }
        int answer;
        if (1 >= currentFrame.body.length - frameBodyIndex) {
            answer = currentFrame.body[frameBodyIndex] & 0xFF;
            currentFrame = null;
            frameBodyIndex = 0;
        } else {
            answer = currentFrame.body[frameBodyIndex++] & 0xFF;
        }
        return answer;
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     * </p>
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     * </p>
     * <p> The first byte read is stored into element <code>b[off]</code>, the
     * next one into <code>b[off+1]</code>, and so on. The number of bytes read
     * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     * </p>
     * <p> In every case, elements <code>b[0]</code> through
     * <code>b[off]</code> and elements <code>b[off+len]</code> through
     * <code>b[b.length-1]</code> are unaffected.
     * </p>
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in array <code>b</code>
     *            at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the stream has been reached.
     * @throws IOException               If the first byte cannot be read for any reason
     *                                   other than end of file, or if the input stream has been closed, or if
     *                                   some other I/O error occurs.
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @see InputStream#read()
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        } else if (len < 0 || off < 0 || len + off > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (ConnState.ConnType.CLOSED == connState.connType) {
            return -1;
        }
        synchronized (this) {
            while (currentFrame == null && connState.connType != ConnState.ConnType.CLOSED && frameQueue.size() == 0) {
                frameBodyIndex = 0;
                try {
                    wait(10000L);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            if (ConnState.ConnType.CLOSED == connState.connType) {
                return -1;
            }
            if (currentFrame == null) {
                currentFrame = frameQueue.remove(0);
            }
        }
        if (len >= currentFrame.body.length - frameBodyIndex) {
            len = currentFrame.body.length - frameBodyIndex;
            System.arraycopy(currentFrame.body, frameBodyIndex, b, off, len);
            currentFrame = null;
            frameBodyIndex = 0;
        } else {
            System.arraycopy(currentFrame.body, frameBodyIndex, b, off, len);
            frameBodyIndex += len;
        }
        return len;
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. The next invocation
     * might be the same thread or another thread.  A single read or skip of this
     * many bytes will not block, but may read or skip fewer bytes.
     * <p> Note that while some implementations of {@code InputStream} will return
     * the total number of bytes in the stream, many will not.  It is
     * never correct to use the return value of this method to allocate
     * a buffer intended to hold all data in this stream.
     * </p>
     *
     * @return an estimate of the number of bytes that can be read (or skipped
     * over) from this input stream without blocking or {@code 0} when
     * it reaches the end of the input stream.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int available() throws IOException {
        int length = 0;
        if (currentFrame != null) {
            length = currentFrame.body.length - frameBodyIndex;
        }
        for (int i = 0; i < frameQueue.size(); i++) {
            length += frameQueue.get(i).body.length;
        }
        return length;
    }

    /**
     * Produce a String representation of the object.
     *
     * @return String describing the stream and its state
     */
    @Override
    public String toString() {
        return "AX25InputStream[" + connState.paramString() + ']';
    }
}
