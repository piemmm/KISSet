package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.comms.host.parser.Mode;

import java.io.IOException;

/**
 * Ports on a TNC - these are various interfaces that the user may have conneted to in the app and this command
 * allows you to list them
 */
public class Ports extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        write(CR);

        return true;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"p", "ports"};
    }

}
