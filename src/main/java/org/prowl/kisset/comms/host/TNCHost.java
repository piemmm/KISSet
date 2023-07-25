package org.prowl.kisset.comms.host;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.Messages;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.io.*;

/**
 * Each TNC host
 */
public class TNCHost {
    // Logging
    private static final Log LOG = LogFactory.getLog("TNCHost");

    // The end character for the TNC prompt
    public static final String CR = CommandParser.CR;

    // The command parser
    private CommandParser parser;

    // Terminal window input stream
    private InputStream in;
    // Terminal window output stream
    private OutputStream out;

    /**
     * Create a new TNC host.
     * @param config The configuration for this host.
     * @param in The input stream for the terminal window.
     * @param out The output stream for the terminal window.
     */
    public TNCHost(HierarchicalConfiguration config, InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.parser = new CommandParser(this);

        start();
    }

    // Start the TNC host
    public void start() {
        try {
            // Write welcome/init message to the client.
            send(CR);
            send(Messages.get("tncInit")+CR);
            send(parser.getPrompt());

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

        } catch(IOException e) {
            LOG.error(e.getMessage(), e);
        }
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
     * @throws IOException
     */
    public void flush() throws IOException {
        out.flush();
    }


}
