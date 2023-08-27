package org.prowl.kisset.comms.host.parser.misc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.ConnState;
import org.prowl.ax25.ConnectionEstablishmentListener;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.comms.host.parser.ExtensionState;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.io.Stream;
import org.prowl.kisset.io.StreamState;
import org.prowl.kisset.util.Tools;
import org.prowl.kisset.util.compression.deflate.DeflateOutputStream;
import org.prowl.kisset.util.compression.deflate.InflateInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamConnectionEstablishmentListener implements ConnectionEstablishmentListener {
    private static final Log LOG = LogFactory.getLog("StreamConnectionEstablishmentListener");

    public static final String CR = CommandParser.CR;

    private CommandParser commandParser;

    private Stream stream;


    public StreamConnectionEstablishmentListener(CommandParser commandParser, Stream stream) {
        this.commandParser = commandParser;
        this.stream = stream;
    }

    @Override
    public void connectionEstablished(Object sessionIdentifier, ConnState conn) {
        try {
            // If our state prevents us from connecting, then we need to abort. (ie: connection is torn down and this listener is no longer valid)
            if (stream.getRemoteCall() == null) {
                return;
            }
            commandParser.writeToTerminal("*** Connected to " + conn.getDst().toString().toUpperCase() + CR);
            commandParser.setModeIfCurrentStream(Mode.CONNECTED_TO_STATION, stream);
            commandParser.getCurrentInterface().getCurrentStream().setStreamState(StreamState.CONNECTED);

            // Now setup the streams so we talk to the station instead of the command parser
            stream.setIOStreams(conn.getInputStream(), conn.getOutputStream());

            OutputStream out = stream.getOutputStream();

            // Create a thread to read from the station and send to the client
            // We are interested in the first line, in case it contains a special header
            // that we need to process to enable more capabilities.
            commandParser.setDivertStream(out);
            Tools.runOnThread(() -> {
                try {
                    StringBuffer responseString = new StringBuffer();
                    while (true) {
                        InputStream in = stream.getInputStream();
                        if (in.available() > 0) {
                            int b = in.read();
                            if (b == -1) {
                                break;
                            }

                            if (b == 13) {

                                // If this is set then we've accepted some of the stations capabilities and we are now waiting for the
                                // response to our 'enable these extensions' command
                                if (stream.getExtensionState() == ExtensionState.NEGOTIATING) {
                                    LOG.debug("Response string: " + responseString + "(response)");
                                    // Look for and respond to the [EXTN <capabilities>] response containing stuff the server enabled.
                                    if (responseString.toString().matches("\\[EXTN [a-zA-Z0-9]+\\]")) {
                                        LOG.debug("Response string: " + responseString + "(matches)");

                                        // We can assume everything is enabled after this response is received.
                                        enableExtensions(responseString.toString());
                                        stream.setExtensionState(ExtensionState.ENABLED);
                                    } else {
                                        // First line was not a match, so we can assume no extensions are enabled.
                                        LOG.debug("Response string: " + responseString + "(no match)");
                                        //stream.setExtensionState(ExtensionState.NOT_SUPPORTED);
                                    }
                                }

                                // Get the first line from our connected station - this will contain [EXTN <capabilities>] if it supports it
                                if (stream.getExtensionState() == ExtensionState.NONE) {
                                    checkRemoteStationCapabilities(responseString.toString());
                                    stream.setExtensionState(ExtensionState.NEGOTIATING);
                                    responseString = new StringBuffer();
                                }


                                // Send a newline to the terminal screen on our computer
                                //    commandParser.writeToTerminal("\r");
                                responseString.delete(0, responseString.length());
                            }

                            String data = String.valueOf((char) b); // Inefficient for now.
                            // Build our response line if negotiating a connection.
                            if ((stream.getExtensionState() == ExtensionState.NONE || stream.getExtensionState() == ExtensionState.NEGOTIATING) && b > 13) {
                                responseString.append(data);
                            }

                            commandParser.writeRawToTerminal(b);
                        } else {
                            // Crude.
                            Tools.delay(100);
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Error reading from client:" + e.getMessage(), e);
                }
                commandParser.setDivertStream(null);
                commandParser.setModeIfCurrentStream(Mode.CMD, stream);

            });
            commandParser.updateStatus();
        } catch (IOException e) {
            commandParser.setDivertStream(null);
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }
    }

    @Override
    public void connectionNotEstablished(Object sessionIdentifier, Object reason) {
        try {
            stream.setStreamState(StreamState.DISCONNECTED);
            commandParser.writeToTerminal("*** Unable to connect: " + reason + CR);
            commandParser.setModeIfCurrentStream(Mode.CMD, stream, true);
            commandParser.updateStatus();
        } catch (IOException e) {
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    @Override
    public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {
        try {
            stream.setStreamState(StreamState.DISCONNECTED);

            commandParser.writeToTerminal("*** Connection closed" + CR);
            commandParser.setModeIfCurrentStream(Mode.CMD, stream, true);
            commandParser.updateStatus();
        } catch (IOException e) {
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    @Override
    public void connectionLost(Object sessionIdentifier, Object reason) {
        try {
            stream.setStreamState(StreamState.DISCONNECTED);
            commandParser.writeToTerminal("*** Lost connection: " + reason + CR);
            commandParser.setModeIfCurrentStream(Mode.CMD, stream, true);
            commandParser.updateStatus();
        } catch (IOException e) {
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    /**
     * If the first line matches a known format then we can enable more capabilities in the client such as compression
     * (which is also enabled serverside)
     * The format of the line we look for is: [EXTN-<capabilities as [a-zA-Z0-9]>] as a regexp
     *
     * @param firstLine
     */
    public void checkRemoteStationCapabilities(String firstLine) throws IOException {
        String capabilities = "";
        StringBuffer response = new StringBuffer();
        if (firstLine.matches("\\[EXTN [a-zA-Z0-9]+\\]")) {
            capabilities = firstLine.substring(6, firstLine.length() - 1);
            stream.setExtensionState(ExtensionState.NEGOTIATING);
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

            response.insert(0, "[EXTN ");
            response.append("]\r");
            LOG.debug("Sending capabilities response: " + response);
            stream.write(response.toString().getBytes());
            stream.flush();
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
            DeflateOutputStream out = new DeflateOutputStream(stream.getOutputStream());
            InflateInputStream in =new InflateInputStream(stream.getInputStream());
            stream.setIOStreams(in, out);
            commandParser.setDivertStream(out);

            Tools.delay(200);
        }

        // All done! Negotiation is complete and extensions are enabled
        LOG.info("Enabled station capabilities: " + capabilities);
    }


}
