package org.prowl.kisset.util.compression.deflate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Decompress an input stream using deflate and a dictionary that updates after each block of data
 */
public class InflateInputStream extends InputStream {

    // A logger for this class
    private static final Log LOG = LogFactory.getLog("InflateInputStream");

    // The input stream containing the compressed data
    private InputStream in;

    // Buffer of decompressed data
    private ByteBuffer byteBuffer = ByteBuffer.allocate(0);

    // The inflater used to decompress the data
    private Inflater inflater;

    // The dictionary used to decompress the data which is updated after each block
    private Dictionary dictionary = new Dictionary();

    public InflateInputStream(InputStream in) {
        super();
        this.in = in;
        inflater = new Inflater();
    }

    /**
     * Given an input stream decompress the data using inflate with our adaptive dictionary
     * <p>
     * Each block consists of a size (2 bytes) (maximum 65535 block size) followed by the data, which is compressed using deflate and a dictionary
     * that is updated with the data from the previous block.
     *
     * @throws IOException if an I/O error occurs.
     */
    private void inflate() throws IOException {

        // 0x00 = uncompressed, 0x01 = compressed
        int status = in.read();
        if (status == -1) {
            return;
        }

        // Get the size of the compressed/uncompressed block
        int s1 = in.read();
        int s2 = in.read();

        // Check neither were -1 indicating end of stream
        if ((s1 | s2) < 0) {
            throw new IOException("Unexpected end of stream");
        }

        // Now get the size of the block and load it into a byte buffer
        int size = (s1 << 8) + s2;
        byte[] inData = new byte[size];

        // If we didn't read the full size of the block then we hit the end of the stream
        int actualRead = in.readNBytes(inData, 0, size);
        if (actualRead != size) {
            throw new IOException("Unexpected end of stream");
        }

        // If we are uncompressed, then just use that data.
        if (status == 0x01) {
            // Uncompressed data
            byteBuffer = ByteBuffer.wrap(inData, 0, size);
            dictionary.addToDictionary(inData, 0, size);
            return;
        }

        // If we got this far then decompress the data - MAX_BLOCK_SIZE is the maximum size of the
        // output buffer that will be sent by the compressor
        byte[] output = new byte[DeflateOutputStream.MAX_BLOCK_SIZE];
        int actualBytesInflated = 0;
        inflater.setInput(inData, 0, size);
        try {
            int dataRead = inflater.inflate(output);
            if (dataRead == 0 && inflater.needsDictionary()) {
                inflater.setDictionary(dictionary.getDictionary());
                actualBytesInflated = inflater.inflate(output);
                if (actualBytesInflated == 0) {
                    throw new IOException("Error inflating data");
                }
            }
        } catch (DataFormatException e) {
            throw new IOException("Error decompressing data", e);
        }

        // Data has been read and decompressed, now update the dictionary
        // and reset the inflater for the next block
        inflater.reset();
        dictionary.addToDictionary(output, 0, actualBytesInflated);


        // Now write the decompressed data to the output stream
        byteBuffer = ByteBuffer.wrap(output, 0, actualBytesInflated);

        // A little bit of debug regarding how well we compressed the data
        LOG.debug("Decompressed in=" + size + " bytes, out=" + actualBytesInflated);

    }

    /**
     * Read decompressed bytes from the input stream
     * <p>
     * If there is no data waiting, then we call inflate() which gets the next block of
     * data and decompresses it, also updating the dictionary (which is kept in sync with the
     * deflating output stream)
     *
     * @return the next byte from the decompressed data
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read() throws IOException {

        // When the buffer is empty then we need to inflate the next block of data
        if (byteBuffer.remaining() == 0) {
            inflate();
        }

        // Still no data? Then return -1
        if (byteBuffer.remaining() == 0) {
            return -1;
        }

        // Return the next byte from out decompressed data buffer
        return byteBuffer.get() & 0xFF;
    }


    /**
     * Get an approximation of how many data bytes from this stream can be read.
     *
     * @return an estimate of the number of bytes that can be read
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int available() throws IOException {
        return Math.max(in.available(), byteBuffer.remaining());
    }

}
