package org.prowl.kisset.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This buffer retains the last X bytes of a stream, and overwrites (and pushes the tail forwards) when the end of the
 * stream is reached.
 *
 * It is a quick way to provide what happened in the last X bytes of a stream so you can replay that to popupate a terminal
 * when you swap terminals (crude, but useful)
 */
public final class LoopingCircularBuffer {

    private static final Log LOG = LogFactory.getLog("LoopingCircularBuffer");

    private final byte[] buffer;
    private int head = 1;
    private int tail = 0;
    private int filled = 0;

    public LoopingCircularBuffer(int size) {
        buffer = new byte[size];
    }

    /**
     * Get the current contents of our byte stream
     * @return
     */
    public byte[] getBytes() {
        byte[] b = new byte[filled];
        int count = 0;
        while (count < filled) {
            b[count] = buffer[(tail+count) % buffer.length];
            count++;
        }
        return b;
    }


    /**
     * Put a byte into the stream
     *
     * @param b
     */
    public void put(byte b) {
        buffer[head % buffer.length] = b;
        // avoid int wraparound.


        // Grow until we are the same size.
        if (head % buffer.length == tail % buffer.length) {
           tail = (tail+1) % buffer.length;
        }
        if (filled < buffer.length) {
            filled++;
        }
        head++;
        if (head % buffer.length == 0) {
            head = 0;
        }
    }


}
