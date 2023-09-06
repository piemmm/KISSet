package org.prowl.kisset.userinterface.stdinout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.Tools;

import java.io.InputStream;
import java.io.OutputStream;

public class StdANSI extends StdTerminal {

    private static final Log LOG = LogFactory.getLog("StdANSI");

    private static boolean running = false;

    public StdANSI(InputStream stdIn, OutputStream stdOut) {
        super(stdIn, stdOut);
    }

    public StdANSI() {
        super();
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
            int lastNewlineByte = 0;
            try {
                while (running) {
                    if (tncOut.available() == 0) {
                        stdOut.flush();
                    }
                    int b = tncOut.read();
                    if (b == -1)
                        break;
                    if (b == 0x0a && lastNewlineByte != 0x0d) {
                        stdOut.write(System.lineSeparator().getBytes());
                        lastNewlineByte = b;
                    } else if (b == 0x0d && lastNewlineByte != 0x0a) {
                        stdOut.write(System.lineSeparator().getBytes());
                        lastNewlineByte = b;
                    } else if (b != 0x0d && b != 0x0a) {
                        stdOut.write(b);
                        lastNewlineByte = b;
                    }
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
