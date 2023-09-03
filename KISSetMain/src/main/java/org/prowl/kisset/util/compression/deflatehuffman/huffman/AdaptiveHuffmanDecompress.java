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
import java.io.OutputStream;


/**
 * Decompression application using adaptive Huffman coding.
 * <p>Usage: java AdaptiveHuffmanDecompress InputFile OutputFile</p>
 * <p>This decompresses files generated by the "AdaptiveHuffmanCompress" application.</p>
 */
public final class AdaptiveHuffmanDecompress {

    public static void decompress(BitInputStream in, OutputStream out) throws IOException {
        //int[] initFreqs = new int[257];
        //Arrays.fill(initFreqs, 1);

        FrequencyTable freqs = FrequencyTable.getDefault();//new FrequencyTable(initFreqs);
        HuffmanDecoder dec = new HuffmanDecoder(in);
        dec.codeTree = freqs.buildCodeTree();  // Use same algorithm as the compressor
        int count = 0;  // Number of bytes written to the output file
        while (true) {
            // Decode and write one byte
            int symbol = dec.read();
            if (symbol == 256)  // EOF symbol
                break;
            out.write(symbol);
            count++;

            // Update the frequency table and possibly the code tree
            freqs.increment(symbol);
            if (count < 262144 && shouldUpdate(count))  // Update code tree
                dec.codeTree = freqs.buildCodeTree();
            if (count % 262144 == 262143)  // Reset frequency table
                freqs = FrequencyTable.getDefault();//new FrequencyTable(initFreqs);
        }
    }


    private static boolean shouldUpdate(int x) {
        return x % 40 == 39;
    }


}