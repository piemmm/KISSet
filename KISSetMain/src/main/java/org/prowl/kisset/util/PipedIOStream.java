package org.prowl.kisset.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A *BLOCKING WHEN NO DATA* piped input/output stream. You feed data in, and you get data out.
 *
 * It does not throw pointless exceptions when the writing thread exits.
 */
public class PipedIOStream extends InputStream {

    private static final Log LOG = LogFactory.getLog("PipedIOStream");

    private final BlockingDeque<Byte> queue;
    private final OutputStream out;
    private boolean closed = false;

    public PipedIOStream() {
        queue = new LinkedBlockingDeque<>(1024);
        out = new PipedIOOutputStream();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    @Override
    public int read() throws IOException {
        if (closed)
            return -1;
        try {
            int b =  queue.take() & 0xFF;
            return b;
        } catch (InterruptedException e) {
            return -1;
        }

    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed)
            return -1;

        int i;
        int actLen = 0;
        for (i = 0; i < len; i++) {
            if (queue.size() > 0 || i == 0) {
                try {
                    b[off + actLen] = queue.take();
                    actLen++;
                } catch (InterruptedException e) {
                    return -1;
                }
            } else {
               break;
            }
        }

        return actLen;
    }

    public class PipedIOOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            if (closed)
                throw new IOException("Stream closed");
            try {
                queue.put((byte) b);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void flush() throws IOException {
            if (closed)
                throw new IOException("Stream closed");
            PipedIOStream.this.flush();
        }

        @Override
        public void close() throws IOException {
            PipedIOStream.this.close();
        }

    }

    public void flush() throws IOException{
        // Default does nothing
    }

    @Override
    public int available() throws IOException {
        return queue.size();
    }

    public OutputStream getOutputStream() {
        return out;
    }
}
