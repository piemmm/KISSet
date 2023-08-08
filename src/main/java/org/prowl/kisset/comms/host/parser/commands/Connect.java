package org.prowl.kisset.comms.host.parser.commands;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.comms.host.parser.misc.StreamConnectionEstablishmentListener;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.io.Stream;
import org.prowl.kisset.io.StreamState;

import java.io.IOException;

/**
 * Connect to a remote station and then the TNC will be in a connect mode.
 */
@TNCCommand
public class Connect extends Command {

    private static final Log LOG = LogFactory.getLog("Connect");


    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        if (commandParser.getCurrentInterface() == null) {
            writeToTerminal("*** No interfaces configured" + CR);
            return true;
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
            writeToTerminal("*** Usage: connect [<port number>] <station>" + CR);
            return true;
        }

        Interface anInterface = commandParser.getCurrentInterface();
        if (anInterface == null) {
            writeToTerminal("*** No interfaces configured" + CR);
            return true;
        }

        Stream currentStream = anInterface.getCurrentStream();
        // connect: C GB7MNK
        if (data.length == 2) {
            String station = data[1].toUpperCase();
            writeToTerminal("*** Connecting to " + station + CR);
            currentStream.setRemoteCall(station);
            anInterface.connect(data[1].toUpperCase(), KISSet.INSTANCE.getMyCall(), new StreamConnectionEstablishmentListener(commandParser, stream));
            anInterface.getCurrentStream().setStreamState(StreamState.CONNECTING);

        } else if (data.length == 3) {
            // Connect: C 2 GB7MNK (to connect via port 2)
            String station = data[2].toUpperCase();
            writeToTerminal("*** Connecting to " + station + CR);
            anInterface.getCurrentStream().setStreamState(StreamState.CONNECTING);
            currentStream.setRemoteCall(station);
            anInterface.connect(data[2].toUpperCase(), KISSet.INSTANCE.getMyCall(), new StreamConnectionEstablishmentListener(commandParser, stream));

        }

        writeToTerminal(CR);

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"c", "connect"};
    }


}
