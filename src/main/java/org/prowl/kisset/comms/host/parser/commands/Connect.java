package org.prowl.kisset.comms.host.parser.commands;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.ax25.ConnState;
import org.prowl.kisset.ax25.ConnectionEstablishmentListener;
import org.prowl.kisset.comms.host.parser.ExtensionState;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.util.Tools;
import org.prowl.kisset.util.compression.block.CompressedBlockInputStream;
import org.prowl.kisset.util.compression.block.CompressedBlockOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Connect to a remote station and then the TNC will be in a connect mode.
 */
@TNCCommand
public class Connect extends Command implements ConnectionEstablishmentListener {

    private static final Log LOG = LogFactory.getLog("Connect");

    private InputStream in;
    private OutputStream out;

    private ExtensionState extensionState = ExtensionState.NONE;

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        if (data.length < 2) {
            write("*** Usage: connect [<port number>] <station>" + CR);
            return true;
        }

        // connect: C GB7MNK
        if (data.length == 2) {
            String station = data[1];
            Interface anInterface = KISSet.INSTANCE.getInterfaceHandler().getInterface(0);
            if (anInterface == null) {
                write("*** No interfaces configured" + CR);
            } else {
                write("*** Connecting to " + data[1].toUpperCase() + CR);

                anInterface.connect(data[1].toUpperCase(), KISSet.INSTANCE.getMyCall(), this);
            }

        } else if (data.length == 3) {
            // Connect: C 2 GB7MNK (to connect via port 2)
            String station = data[2];
            Interface anInterface = KISSet.INSTANCE.getInterfaceHandler().getInterface(Integer.parseInt(data[1]));
            if (anInterface == null) {
                write("*** No interfaces configured" + CR);
            } else {
                write("*** Connecting to " + data[2].toUpperCase() + CR);

                anInterface.connect(data[2].toUpperCase(), KISSet.INSTANCE.getMyCall(), this);
            }
        }

        write(CR);

        return true;
    }

    @Override
    public void connectionEstablished(Object sessionIdentifier, ConnState conn) {
        try {
            write("*** Connected to " + conn.getDst().toString().toUpperCase() + CR);
            setMode(Mode.CONNECTED_TO_STATION);

            // Now setup the streams so we talk to the station instead of the command parser
            in = conn.getInputStream();
            out = conn.getOutputStream();

            // Create a thread to read from the station and send to the client
            // We are interested in the first line, in case it contains a special header
            // that we need to process to enable more capabilities.
            commandParser.setDivertStream(out);
            Tools.runOnThread(() -> {
                try {
                    StringBuffer responseString = new StringBuffer();
                    while (true) {

                        if (in.available() > 0) {
                            int b = in.read();
                            if (b == -1) {
                                break;
                            }

                            if (b == 13) {

                                // If this is set then we've accepted some of the stations capabilities and we are now waiting for the
                                // response to our 'enable these extensions' command
                                if (extensionState == ExtensionState.NEGOTIATING) {
                                    LOG.debug("Response string: " + responseString + "(response)");
                                    // Look for and respond to the [OARC <capabilities>] response containing stuff the server enabled.
                                    if (responseString.toString().matches("\\[OARC [a-zA-Z0-9]+\\]")) {
                                        LOG.debug("Response string: " + responseString + "(matches)");

                                        // We can assume everything is enabled after this response is received.
                                        enableExtensions(responseString.toString());
                                        extensionState = ExtensionState.ENABLED;
                                    } else {
                                        LOG.debug("Response string: " + responseString + "(no match)");
                                    }
                                }

                                // Get the first line from our connected station - this will contain [OARC <capabilities>] if it supports it
                                if (extensionState == ExtensionState.NONE) {
                                    checkRemoteStationCapabilities(responseString.toString());
                                    extensionState = ExtensionState.NEGOTIATING;
                                    responseString = new StringBuffer();
                                }


                                // Send a newline to the terminal screen on our computer
                                tncHost.send("\n");
                                responseString.delete(0, responseString.length());
                            }
                            String data = String.valueOf((char) b); // Inefficient for now.
                            // Build our response line if negotiating a connection.
                            if ((extensionState == ExtensionState.NONE || extensionState == ExtensionState.NEGOTIATING) && b > 13) {
                                responseString.append(data);
                            }

                            tncHost.send(data);
                        } else {
                            // Crude.
                            Tools.delay(100);
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Error reading from client:" + e.getMessage(), e);
                }
                commandParser.setDivertStream(null);
                setMode(Mode.CMD);

            });
        } catch (IOException e) {
            commandParser.setDivertStream(null);
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }
    }

    @Override
    public void connectionNotEstablished(Object sessionIdentifier, Object reason) {
        try {
            write("*** Unable to connect: " + reason + CR);
            setMode(Mode.CMD, true);
        } catch (IOException e) {
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    @Override
    public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {
        try {
            write("*** Connection closed" + CR);
            setMode(Mode.CMD, true);
        } catch (IOException e) {
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    @Override
    public void connectionLost(Object sessionIdentifier, Object reason) {
        try {
            write("*** Lost connection: " + reason + CR);
            setMode(Mode.CMD, true);
        } catch (IOException e) {
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"c", "connect"};
    }

    /**
     * If the first line matches a known format then we can enable more capabilities in the client such as compression
     * (which is also enabled serverside)
     * The format of the line we look for is: [OARC-<capabilities as [a-zA-Z0-9]>] as a regexp
     *
     * @param firstLine
     */
    public void checkRemoteStationCapabilities(String firstLine) throws IOException {
        String capabilities = "";
        StringBuffer response = new StringBuffer();
        if (firstLine.matches("\\[OARC [a-zA-Z0-9]+\\]")) {
            capabilities = firstLine.substring(6, firstLine.length() - 1);
            extensionState = ExtensionState.NEGOTIATING;
            LOG.info("Remote station capabilities: " + capabilities);
        }

        boolean compression = false;
        if (capabilities.contains("C")) {
            LOG.info("Remote station supports compression");
            compression = true;
            response.append("C");
        }

        // Now we have our responses we can send it to the remote station and consider all of them immediately enabled
        if (response.length() > 0) {

            response.insert(0, "[OARC ");
            response.append("]\r");
            LOG.debug("Sending capabilities response: " + response);
            out.write(response.toString().getBytes());
            out.flush();
        }

    }

    /**
     * Given a response from the server, enable our negotiated extensions
     *
     * @param enabledExtensionsResponse
     * @throws IOException
     */
    public void enableExtensions(String enabledExtensionsResponse) throws IOException {
        LOG.debug("Enabling extensions...");
        // Get the capabilities characters from the response
        String capabilities = enabledExtensionsResponse.substring(6, enabledExtensionsResponse.length() - 1);

        // For compression, we wrap the input and output stream in a gzip stream
        if (capabilities.contains("C")) {
            LOG.info("Enabling compression");
            // The smaller the block size, the more 'interactive', but less compression.
            // The client should provide some form of interactivity when downloading blocks
            // of compressed data.  calling flush() on the stream will cause the block to be sent
            // at it's current size immediately.
            out = new CompressedBlockOutputStream(out, 1024);
            in = new CompressedBlockInputStream(in);
            commandParser.setDivertStream(out);

            Tools.delay(200);
        }

        // All done! Negotiation is complete and extensions are enabled
        LOG.info("Enabled station capabilities: " + capabilities);
    }

}
