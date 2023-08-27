package org.prowl.kisset.services.host.parser.commands;

import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.io.StreamState;

import java.io.IOException;

@TNCCommand
public class Converse extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }
        writeToTerminal(CR);

        if (commandParser.getCurrentInterface() == null) {
            writeToTerminal("*** No interface selected" + CR);
            return true;
        }

        if (commandParser.getCurrentInterface().getCurrentStream() == null) {
            writeToTerminal("*** No stream selected" + CR);
            return true;
        }

        if (!commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTED)) {
            writeToTerminal("*** Not connected to a station" + CR);
            return true;
        }

        // Go back to converse mode (divertStream is already set so we don't need to do that again)
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTED)) {
            commandParser.setMode(Mode.CONNECTED_TO_STATION);
        }

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"k", "conv", "converse"};
    }


}
