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

import java.io.IOException;
import java.util.List;
import java.util.Objects;


/**
 * Encodes symbols and writes to a Huffman-coded bit stream. Not thread-safe.
 *
 * @see HuffmanDecoder
 */
public final class HuffmanEncoder {

    /*---- Fields ----*/

    /**
     * The code tree to use in the next {@link#write(int)} operation. Must be given a non-{@code null}
     * value before calling write(). The tree can be changed after each symbol encoded, as long
     * as the encoder and decoder have the same tree at the same point in the code stream.
     */
    public CodeTree codeTree;
    // The underlying bit output stream (not null).
    private final BitOutputStream output;



    /*---- Constructor ----*/

    /**
     * Constructs a Huffman encoder based on the specified bit output stream.
     *
     * @param out the bit output stream to write to
     * @throws NullPointerException if the output stream is {@code null}
     */
    public HuffmanEncoder(BitOutputStream out) {
        output = Objects.requireNonNull(out);
    }



    /*---- Method ----*/

    /**
     * Encodes the specified symbol and writes to the Huffman-coded output stream.
     *
     * @param symbol the symbol to encode, which is non-negative and must be in the range of the code tree
     * @throws IOException              if an I/O exception occurred
     * @throws NullPointerException     if the current code tree is {@code null}
     * @throws IllegalArgumentException if the symbol value is negative or has no binary code
     */
    public void write(int symbol) throws IOException {
        if (codeTree == null)
            throw new NullPointerException("Code tree is null");
        List<Integer> bits = codeTree.getCode(symbol);
        for (int b : bits)
            output.write(b);
    }

}
