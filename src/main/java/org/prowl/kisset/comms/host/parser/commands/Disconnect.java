package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.io.StreamState;

import java.io.IOException;

@TNCCommand
public class Disconnect extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        // If we're in command mode, then
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.DISCONNECTED)) {
            writeToTerminal("*** Not connected to a station");
            return true;
        }

        // Cancel the current connection attempt on the current interface
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTING)) {
          //TODO  commandParser.getCurrentInterface().cancelConnectionAttempt();
            writeToTerminal("*** Connection attempt cancelled");
            return true;
        }

        // Disconnect the current stream
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTED)) {
            writeToTerminal("*** Disconnecting");
            commandParser.getCurrentInterface().getCurrentStream().disconnect();
            return true;
        }

        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTED)) {
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
