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
import java.util.Objects;


/**
 * Reads from a Huffman-coded bit stream and decodes symbols. Not thread-safe.
 *
 * @see HuffmanEncoder
 */
public final class HuffmanDecoder {

    /*---- Fields ----*/

    /**
     * The code tree to use in the next {@link#read()} operation. Must be given a non-{@code null}
     * value before calling read(). The tree can be changed after each symbol decoded, as long
     * as the encoder and decoder have the same tree at the same point in the code stream.
     */
    public CodeTree codeTree;
    // The underlying bit input stream (not null).
    private final BitInputStream input;



    /*---- Constructor ----*/

    /**
     * Constructs a Huffman decoder based on the specified bit input stream.
     *
     * @param in the bit input stream to read from
     * @throws NullPointerException if the input stream is {@code null}
     */
    public HuffmanDecoder(BitInputStream in) {
        input = Objects.requireNonNull(in);
    }



    /*---- Method ----*/

    /**
     * Reads from the input stream to decode the next Huffman-coded symbol.
     *
     * @return the next symbol in the stream, which is non-negative
     * @throws IOException          if an I/O exception occurred
     * @throws EOFException         if the end of stream was reached before a symbol was decoded
     * @throws NullPointerException if the current code tree is {@code null}
     */
    public int read() throws IOException {
        if (codeTree == null)
            throw new NullPointerException("Code tree is null");

        InternalNode currentNode = codeTree.root;
        while (true) {
            int temp = input.readNoEof();
            Node nextNode;
            if (temp == 0) nextNode = currentNode.leftChild;
            else if (temp == 1) nextNode = currentNode.rightChild;
            else throw new AssertionError("Invalid value from readNoEof()");

            if (nextNode instanceof Leaf)
                return ((Leaf) nextNode).symbol;
            else if (nextNode instanceof InternalNode)
                currentNode = (InternalNode) nextNode;
            else
                throw new AssertionError("Illegal node type");
        }
    }

}
