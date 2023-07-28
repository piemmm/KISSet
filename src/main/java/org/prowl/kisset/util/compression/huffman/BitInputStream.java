/*
 * Reference Huffman coding
 * Copyright (c) Project Nayuki
 *
 * https://www.nayuki.io/page/reference-huffman-coding
 * https://github.com/nayuki/Reference-Huffman-coding
 *
 * License
 * Copyright © 2018 Project Nayuki. (MIT License)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * The Software is provided "as is", without warranty of any kind, express or implied, including but not
 * limited to the warranties of merchantability, fitness for a particular purpose and noninfringement. In
 * no event shall the authors or copyright holders be liable for any claim, damages or other liability,
 * whether in an action of contract, tort or otherwise, arising from, out of or in connection with the
 * Software or the use or other dealings in the Software.
 */
package org.prowl.kisset.util.compression.huffman;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


/**
 * A stream of bits that can be read. Because they come from an underlying byte stream,
 * the total number of bits is always a multiple of 8. The bits are read in big endian.
 * Mutable and not thread-safe.
 *
 * @see BitOutputStream
 */
public final class BitInputStream implements AutoCloseable {

    /*---- Fields ----*/

    // The underlying byte stream to read from (not null).
    private final InputStream input;

    // Either in the range [0x00, 0xFF] if bits are available, or -1 if end of stream is reached.
    private int currentByte;

    // Number of remaining bits in the current byte, always between 0 and 7 (inclusive).
    private int numBitsRemaining;



    /*---- Constructor ----*/

    /**
     * Constructs a bit input stream based on the specified byte input stream.
     *
     * @param in the byte input stream
     * @throws NullPointerException if the input stream is {@code null}
     */
    public BitInputStream(InputStream in) {
        input = Objects.requireNonNull(in);
        currentByte = 0;
        numBitsRemaining = 0;
    }



    /*---- Methods ----*/

    /**
     * Reads a bit from this stream. Returns 0 or 1 if a bit is available, or -1 if
     * the end of stream is reached. The end of stream always occurs on a byte boundary.
     *
     * @return the next bit of 0 or 1, or -1 for the end of stream
     * @throws IOException if an I/O exception occurred
     */
    public int read() throws IOException {
        if (currentByte == -1)
            return -1;
        if (numBitsRemaining == 0) {
            currentByte = input.read();
            if (currentByte == -1)
                return -1;
            numBitsRemaining = 8;
        }
        if (numBitsRemaining <= 0)
            throw new AssertionError();
        numBitsRemaining--;
        return (currentByte >>> numBitsRemaining) & 1;
    }


    /**
     * Reads a bit from this stream. Returns 0 or 1 if a bit is available, or throws an {@code EOFException}
     * if the end of stream is reached. The end of stream always occurs on a byte boundary.
     *
     * @return the next bit of 0 or 1
     * @throws IOException  if an I/O exception occurred
     * @throws EOFException if the end of stream is reached
     */
    public int readNoEof() throws IOException {
        int result = read();
        if (result != -1)
            return result;
        else
            throw new EOFException();
    }


    /**
     * Closes this stream and the underlying input stream.
     *
     * @throws IOException if an I/O exception occurred
     */
    public void close() throws IOException {
        input.close();
        currentByte = -1;
        numBitsRemaining = 0;
    }

}
