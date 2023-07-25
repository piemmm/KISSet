package org.prowl.kisset.comms.host;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.io.*;

public class TNCHost {

    private static final Log LOG = LogFactory.getLog("TNCHost");

    public static final String CR = CommandParser.CR;

    private CommandParser parser;

    private InputStream in;
    private OutputStream out;

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
            for (int i = 0; i < 100; i++) {
                send(CR);
            }
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

        // Strip colour if needed.
       // data = ANSI.convertTokensToANSIColours(data);

        out.write(data.getBytes());
    }

    public void flush() throws IOException {
        out.flush();
    }


}
