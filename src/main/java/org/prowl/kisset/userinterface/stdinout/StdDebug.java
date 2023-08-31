package org.prowl.kisset.userinterface.stdinout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.Tools;

import java.io.InputStream;
import java.io.OutputStream;

public class StdDebug extends StdANSI {

    private static final Log LOG = LogFactory.getLog("StdANSI");

    private static boolean running = false;

    public StdDebug(InputStream stdIn, OutputStream stdOut) {
        super(stdIn, stdOut);
    }

    // Default std terminal is just a passthrough.
    public void start() {
        running = true;
        // Take std input and pass to TNC host
        Tools.runOnThread(() -> {
            try {
                while (running) {
                    int b = stdIn.read();
                    if (b == -1)
                        break;
                    tncIn.getOutputStream().write(b);
                }
                running = false;
            } catch (Throwable e) {
                LOG.debug(e.getMessage(), e);
            }
        });

        // Take output from TNC host and pass to stdout
        Tools.runOnThread(() -> {
            try {
                while (running) {
                    if (tncOut.available() == 0) {
                        stdOut.flush();
                    }
                    int b = tncOut.read();
                    if (b == -1)
                        break;
                    stdOut.write(b);
                }
                running = false;
            } catch (Throwable e) {
                LOG.debug(e.getMessage(), e);
            }
        });

    }

    public void stop() {
        running = false;
    }
}
