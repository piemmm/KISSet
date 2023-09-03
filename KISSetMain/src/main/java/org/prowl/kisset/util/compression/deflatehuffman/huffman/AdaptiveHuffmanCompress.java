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
package org.prowl.kisset.util.compression.deflatehuffman.huffman;

import java.io.IOException;
import java.io.InputStream;

/**
 * Compression application using adaptive Huffman coding.
 * <p>
 * Usage: java AdaptiveHuffmanCompress InputFile OutputFile
 * </p>
 * <p>
 * Then use the corresponding "AdaptiveHuffmanDecompress" application to
 * recreate the original input file.
 * </p>
 * <p>
 * Note that the application starts with a flat frequency table of 257 symbols
 * (all set to a frequency of 1), collects statistics while bytes are being
 * encoded, and regenerates the Huffman code periodically. The corresponding
 * decompressor program also starts with a flat frequency table, updates it
 * while bytes are being decoded, and regenerates the Huffman code periodically
 * at the exact same points in time. It is by design that the compressor and
 * decompressor have synchronized states, so that the data can be decompressed
 * properly.
 * </p>
 */
public final class AdaptiveHuffmanCompress {

    // To allow unit testing, this method is package-private instead of private.
    public static void compress(InputStream in, BitOutputStream out) throws IOException {
        // int[] initFreqs = new int[257];
        // Arrays.fill(initFreqs, 1);

        FrequencyTable freqs = FrequencyTable.getDefault();// new FrequencyTable(initFreqs);
        HuffmanEncoder enc = new HuffmanEncoder(out);
        enc.codeTree = freqs.buildCodeTree(); // Don't need to make canonical code because we don't transmit the code tree
        int count = 0; // Number of bytes read from the input file
        while (true) {
            // Read and encode one byte
            int symbol = in.read() & 0xFF;
            if (symbol == -1)
                break;
            enc.write(symbol);
            count++;

            // Update the frequency table and possibly the code tree
            freqs.increment(symbol);
            if (count < 262144 && shouldUpdate(count)) // Update code tree
                enc.codeTree = freqs.buildCodeTree();
            if (count % 262144 == 262143) // Reset frequency table
                freqs = FrequencyTable.getDefault();// new FrequencyTable(initFreqs);
        }
        enc.write(256); // EOF
    }

    private static boolean shouldUpdate(int x) {
        return x % 40 == 39;
    }


}
