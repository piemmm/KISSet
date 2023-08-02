package org.prowl.kisset.comms.host;

import javafx.application.Platform;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.Messages;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.io.*;
import java.util.List;

/**
 * Each TNC host
 */
public class TNCHost {
    // The end character for the TNC prompt
    public static final String CR = CommandParser.CR;
    // Logging
    private static final Log LOG = LogFactory.getLog("TNCHost");
    // The command parser
    private final CommandParser parser;

    // Terminal window input stream
    private final InputStream in;
    // Terminal window output stream
    private final OutputStream out;

    /**
     * Create a new TNC host.
     *
     * @param in     The input stream for the terminal window.
     * @param out    The output stream for the terminal window.
     */
    public TNCHost(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.parser = new CommandParser(this);
        start();
    }

    // Start the TNC host
    public void start() {


        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                try {
                    // Write welcome/init message to the client.
                    for (int i = 0; i < 100; i++) {
                        send(CR);
                    }
                    send(Messages.get("tncInit") + CR);

                    checkConfiguration();

                    send(Messages.get("tncHelp") + CR);

                    send(parser.getPrompt());
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });

        // Start the reader thread for the client.
        Tools.runOnThread(() -> {
            try {
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader bin = new BufferedReader(reader);
                String inLine;
                while ((inLine = bin.readLine()) != null) {
                    parser.parse(inLine);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });


    }

    /**
     * Send ASCII text data to the client - will strip colour codes if user has requested it.
     *
     * @param data
     * @throws IOException
     */
    public void send(String data) throws IOException {
        data = data.replaceAll("[^\\x04-\\xFF]", "?");

        // Colourise
        data = ANSI.convertTokensToANSIColours(data);

        out.write(data.getBytes());
    }

    /**
     * Flush the output stream.
     *
     * @throws IOException
     */
    public void flush() throws IOException {
        out.flush();
    }


    public void checkConfiguration() {
        // Go through each interface and check for a fail reason
        // If there is a fail reason, then we need to display a warning to the user.
        List<Interface> interfaces = KISSet.INSTANCE.getInterfaceHandler().getInterfaces();
        for (Interface iface : interfaces) {
            String failReason = iface.getFailReason();
            if (failReason != null) {
                try {
                    send("*** " + failReason + CR);
                } catch(IOException e) {

                }
            }
        }
    }

}
