package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.io.Stream;
import org.prowl.kisset.io.StreamState;

import java.io.IOException;

@TNCCommand
public class Disconnect extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        if (data.length > 1 && data[1].equals("?")) {
            writeToTerminal("*** Usage: disconnect" + CR);
            return true;
        }

        if (commandParser.getCurrentInterface() == null) {
            writeToTerminal("*** No interfaces configured" + CR);
            return true;
        }

        // Get the current stream we are using
        Stream currentStream = commandParser.getCurrentInterface().getCurrentStream();

        // If we're in command mode, then
        if (currentStream.getStreamState().equals(StreamState.DISCONNECTED)) {
            writeToTerminal("*** Not connected to a station");
            return true;
        }

        // Cancel the current connection attempt on the current interface
        if (currentStream.getStreamState().equals(StreamState.CONNECTING)) {
            commandParser.getCurrentInterface().cancelConnection(currentStream);
            writeToTerminal("*** Connection attempt cancelled");
            return true;
        }

        // Disconnect the current stream
        if (currentStream.getStreamState().equals(StreamState.CONNECTED)) {
            writeToTerminal("*** Disconnecting");
            currentStream.disconnect();
            return true;
        }

        if (currentStream.getStreamState().equals(StreamState.DISCONNECTING)) {
            currentStream.disconnectNow();
            writeToTerminal("*** Disconnected");
            return true;
        }

        commandParser.closeDivertStream();
        commandParser.setMode(Mode.CMD);
        writeToTerminal(CR);

        return true;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"disc", "d", "disconnect"};
    }

}
