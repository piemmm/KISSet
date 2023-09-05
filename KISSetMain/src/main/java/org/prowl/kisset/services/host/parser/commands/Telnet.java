package org.prowl.kisset.services.host.parser.commands;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.io.Stream;
import org.prowl.kisset.io.StreamState;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Connect to a remote IP address and port
 */
@TNCCommand
public class Telnet extends Command {

    private static final Log LOG = LogFactory.getLog("Telnet");


    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        Stream stream = commandParser.getCurrentInterface().getCurrentStream();

        // If this stream is already connected, then the user must disconnect first
        if (stream.getStreamState().equals(StreamState.CONNECTED)) {
            writeToTerminal("*** Already connected to a station" + CR);
            return false;
        }

        // Same for if the user is already in a connecting state
        if (stream.getStreamState().equals(StreamState.CONNECTING)) {
            writeToTerminal("*** Already connecting to a station" + CR);
            return false;
        }

        if (data.length < 2 || data[1].equals("?")) {
            writeToTerminal("*** Usage: telnet <hostname> <port>" + CR);
            return true;
        }


        // Get the ip/host port
        String hostname = data[1];
        int port = Tools.getInteger(data[2], -1);
        if (port == -1) {
            writeToTerminal("*** Invalid port" + CR);
            return true;
        }

        // Try to connect
        try {
            Socket s = new Socket(InetAddress.getByName(hostname), port);
            try {
                s.setSoTimeout(60000);
            } catch (Throwable e) {
            }
            try {
                s.setKeepAlive(true);
            } catch (Throwable e) {
            }

            // Now connect the streams to our terminal
            commandParser.writeToTerminal("*** Connected to " + hostname + ":" + port + CR);
            commandParser.setModeIfCurrentStream(Mode.CONNECTED_TO_STATION, stream);
            commandParser.getCurrentInterface().getCurrentStream().setStreamState(StreamState.CONNECTED);

            // Now setup the streams so we talk to the station instead of the command parser
            stream.setIOStreams(s.getInputStream(), s.getOutputStream());

            OutputStream out = stream.getOutputStream();

            // Create a thread to read from the station and send to the client
            // We are interested in the first line, in case it contains a special header
            // that we need to process to enable more capabilities.
            commandParser.setDivertStream(out);

            // Look for any terminal changing teletext (etc) stuff
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
        } catch (Throwable e) {
            writeToTerminal("*** Unable to connect to " + hostname + ":" + port + " - " + e.getMessage() + CR);
            return true;
        }

        writeToTerminal(CR);

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"telnet"};
    }


}
