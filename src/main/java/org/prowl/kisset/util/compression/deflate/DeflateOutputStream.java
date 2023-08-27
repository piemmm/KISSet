package org.prowl.kisset.util.compression.deflate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * Compress an output stream using deflate and a dictionary that updates after each block of data
 */
public class DeflateOutputStream extends OutputStream {

    /**
     * A logger for this class
      */
    private static final Log LOG = LogFactory.getLog("DeflateOutputStream");

    /**
     * The maximum size of a block of data - lower values mean
     * less compressibility, but more interactive states.
     *
     * Higher values mean you can compress large data blocks more effectively,
     * but this means that the dictionary is not updated as often, and also the
     * clients interactivity will also suffer.
     *
     * Around 1k seems to be a good compromise between the two with the best compression
     * and good interactivity (and dictionary updates)
     */
    public static final int MAX_BLOCK_SIZE = 1024;

    /**
     * The output stream to write the compressed data to
     */
    private OutputStream out;

    /**
     * The deflater used to compress the data
     */
    private final Deflater deflater;
    /**
     * The dictionary used to compress the data which is updated after each block
     */
    private final Dictionary dictionary;

    /**
     * This is our compressed data output
     * @param out
     */
    private ByteArrayOutputStream dataToCompress = new ByteArrayOutputStream();

    /**
     * Create a new deflate output stream
     * @param out the output stream to write compressed data to
     */
    public DeflateOutputStream(OutputStream out) {
        this.out = out;
        dictionary = new Dictionary();

        // Create the deflater with the best compression and default strategy
        deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setStrategy(Deflater.DEFAULT_STRATEGY);
        deflater.setLevel(Deflater.BEST_COMPRESSION);
        deflater.setDictionary(dictionary.getDictionary());
        deflater.reset();
    }


    /**
     * Compress the supplied data
     * @param b   the {@code byte}.
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
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {

        // Nothing to send? then return
        if (dataToCompress.size() == 0) {
            return;
        }

        // Compress the data in our buffer
        byte[] toCompress = dataToCompress.toByteArray();
        deflater.setInput(toCompress,0, toCompress.length);
        deflater.finish();

        deflater.setDictionary(dictionary.getDictionary());
        byte[] output = new byte[MAX_BLOCK_SIZE + 64];
        int bytesCompressed = deflater.deflate(output);

        // Now we check that the data actually compressed - if it didn't then we just send the data uncompressed
        if (bytesCompressed == 0 || bytesCompressed > dataToCompress.size()) {
            LOG.debug("Data did not compress("+bytesCompressed+">"+dataToCompress.size()+"), sending uncompressed");
            sendUncompressed(out);
        } else {
            sendCompressed(out, bytesCompressed, output);
        }

        // Flush the output to make sure the data sent.
        out.flush();

        // Reset the deflater for the next round of compression.
        deflater.reset();

        // Add this to the dictionary
        dictionary.addToDictionary(toCompress, 0, toCompress.length);
        dataToCompress.reset();
    }

    public void sendUncompressed(OutputStream out) throws IOException {
        // Status byte
        out.write(0x01); // 0x01 = uncompressed
        // Write the size of the uncompressed data
        out.write((dataToCompress.size() >>  8) & 0xFF);
        out.write(dataToCompress.size() & 0xFF);

        // Now send the uncompressed data out.
        out.write(dataToCompress.toByteArray());

        // Write a log about the compression
        LOG.debug("Uncompressed out=" + dataToCompress.size());
    }

    public void sendCompressed(OutputStream out, int bytesCompressed, byte[] output) throws IOException {
        // Status byte
        out.write(0x00); // 0x00 = compressed
        // Write the size of the compressed data
        out.write((bytesCompressed >>  8) & 0xFF);
        out.write(bytesCompressed & 0xFF);

        // Now send the compressed data out.
        out.write(output,0,bytesCompressed);

        // Write a log about the compression
        LOG.debug("Compressed in=" + dataToCompress.size() + " bytes, out=" + bytesCompressed + ", reduction(%): " + (100 - (bytesCompressed * 100 / dataToCompress.size())));
    }
}
