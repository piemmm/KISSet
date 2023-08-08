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
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.DISCONNECTED)) {
            writeToTerminal("*** Not connected to a station");
            return true;
        }

        // Cancel the current connection attempt on the current interface
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTING)) {
            commandParser.getCurrentInterface().cancelConnection(currentStream);
            writeToTerminal("*** Connection attempt cancelled");
            return true;
        }

        // Disconnect the current stream
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTED)) {
            writeToTerminal("*** Disconnecting");
            commandParser.getCurrentInterface().getCurrentStream().disconnect();
            commandParser.getCurrentInterface().disconnect(currentStream);
            return true;
        }

        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.DISCONNECTING)) {
            commandParser.getCurrentInterface().getCurrentStream().disconnectNow();
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
