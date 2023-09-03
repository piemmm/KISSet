package org.prowl.kisset.util.compression.deflatehuffman;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.compression.deflate.Dictionary;
import org.prowl.kisset.util.compression.deflatehuffman.huffman.BitOutputStream;
import org.prowl.kisset.util.compression.deflatehuffman.huffman.FrequencyTable;
import org.prowl.kisset.util.compression.deflatehuffman.huffman.HuffmanEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * Compress an output stream using deflate and a dictionary that updates after each block of data
 */
public class DeflateHuffmanOutputStream extends OutputStream {

    /**
     * The maximum size of a block of data - lower values mean
     * less compressibility, but more interactive states.
     * <p>
     * Higher values mean you can compress large data blocks more effectively,
     * but this means that the dictionary is not updated as often, and also the
     * clients interactivity will also suffer.
     * <p>
     * Around 1k seems to be a good compromise between the two with the best compression
     * and good interactivity (and dictionary updates)
     * <p>
     * The maximum permissible size is 64k
     */
    public static final int MAX_BLOCK_SIZE = 1024;
    /**
     * A logger for this class
     */
    private static final Log LOG = LogFactory.getLog("DeflateHuffmanOutputStream");
    /**
     * The deflater used to compress the data
     */
    private final Deflater deflater;
    /**
     * The dictionary used to compress the data which is updated after each block
     */
    private final Dictionary dictionary;
    /**
     * The output stream to write the compressed data to
     */
    private final OutputStream out;
    /**
     * This is our compressed data output
     *
     * @param out
     */
    private final ByteArrayOutputStream dataToCompress = new ByteArrayOutputStream();

    private HuffmanEncoder huffmanEncoder;
    private ByteArrayOutputStream huffmanEncodedOutputStream = new ByteArrayOutputStream();
    private BitOutputStream bitOutputStream;
    private FrequencyTable freqs;

    /**
     * Create a new deflate output stream
     *
     * @param out the output stream to write compressed data to
     */
    public DeflateHuffmanOutputStream(OutputStream out) {
        this.out = out;
        dictionary = new Dictionary();

        // Create the deflater with the best compression and default strategy
        deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setStrategy(Deflater.DEFAULT_STRATEGY);
        deflater.setLevel(Deflater.BEST_COMPRESSION);
        deflater.setDictionary(dictionary.getDictionary());
        deflater.reset();

        // Create the huffman deflater
        freqs = FrequencyTable.getDefault();// new FrequencyTable(initFreqs);
        bitOutputStream = new BitOutputStream(huffmanEncodedOutputStream);
        huffmanEncoder = new HuffmanEncoder(bitOutputStream);
        huffmanEncoder.codeTree = freqs.buildCodeTree();

    }


    /**
     * Compress the supplied data
     *
     * @param b the {@code byte}.
     * @throws IOException
     */
    @Override
    public void write(int b) throws IOException {
        dataToCompress.write(b);
        if (dataToCompress.size() >= MAX_BLOCK_SIZE) {
            flush();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            dataToCompress.write(b[off + i]);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        for (int i = 0; i < b.length; i++) {
            dataToCompress.write(b[i]);
        }
    }

    /**
     * Causes the current buffer to be compressed and sent to the output stream
     *
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {

        // Nothing to send? then return
        if (dataToCompress.size() == 0) {
            return;
        }

        // Compress the data in our buffer using deflate
        byte[] toCompress = dataToCompress.toByteArray();
        deflater.setInput(toCompress, 0, toCompress.length);
        deflater.finish();

        deflater.setDictionary(dictionary.getDictionary());
        byte[] output = new byte[MAX_BLOCK_SIZE + 64];
        int bytesCompressed = deflater.deflate(output);


        // Compress the same data using huffman.
        huffmanEncoder.codeTree = freqs.buildCodeTree();
        // Now huffmanCompress the stream
        for (int i = 0; i < toCompress.length; i++) {
            int b = toCompress[i] & 0xFF;
            huffmanEncoder.write(b);
            // Update the frequency table and possibly the code tree
            freqs.increment(b);
            huffmanEncoder.codeTree = freqs.buildCodeTree();
        }
        huffmanEncoder.write(256);  // EOF
        bitOutputStream.close(); // There is a possible byte saving here by simply removing this statement so no padding is done and accepting the EOF

        // See which ended up smaller!
        //LOG.debug("Deflate:" + bytesCompressed + "    Huffman:" + huffmanEncodedOutputStream.size() + "    None:" + dataToCompress.size());

        // Now we check that the data actually compressed - if it didn't then we just send the data uncompressed
        if (huffmanEncodedOutputStream.size() < bytesCompressed && huffmanEncodedOutputStream.size() < dataToCompress.size()) {
            // Huffman was better
            bytesCompressed = huffmanEncodedOutputStream.size();
            output = huffmanEncodedOutputStream.toByteArray();
            sendData(out, bytesCompressed, output, Compression.HUFFMAN);
        } else if (bytesCompressed < dataToCompress.size()) {
            // Deflate was better
            sendData(out, bytesCompressed, output, Compression.DEFLATE);
        } else {
            // Nothing was better, so send uncompressed.
            sendData(out, dataToCompress.size(), toCompress, Compression.NONE);
        }

        // Flush the output to make sure the data sent.
        out.flush();

        // Reset the huffman deflater
        huffmanEncodedOutputStream.reset();
        bitOutputStream = new BitOutputStream(huffmanEncodedOutputStream);
        huffmanEncoder = new HuffmanEncoder(bitOutputStream);
        huffmanEncoder.codeTree = freqs.buildCodeTree();

        // Reset the deflater for the next round of compression.
        deflater.reset();

        // Add this to the dictionary
        dictionary.addToDictionary(toCompress, 0, toCompress.length);
        dataToCompress.reset();
    }

    /**
     * Send the data to the client
     *
     * @param out             the output stream
     * @param dataLength      length of data to write
     * @param output          the data to write
     * @param compressionType the type of compression used
     * @throws IOException if an I/O error occurs.
     */
    public void sendData(OutputStream out, int dataLength, byte[] output, Compression compressionType) throws IOException {

        // Status byte (compression type)
        out.write(compressionType.getType()); // 0x00 = compressed, etc - see Compression enum

        // Write the size of the compressed data
        out.write((dataLength >> 8) & 0xFF);
        out.write(dataLength & 0xFF);

        // Now send the compressed data out.
        out.write(output, 0, dataLength);
    }

    /**
     * The type of compression used - different compression types are good at different data sizes.
     */
    public enum Compression {
        NONE(0), // No compression
        DEFLATE(1), // Deflate compression
        HUFFMAN(2); // Huffman reference compression

        private int type;

        Compression(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }
}
