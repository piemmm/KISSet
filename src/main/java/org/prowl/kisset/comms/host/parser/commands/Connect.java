package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.comms.host.parser.Mode;

import java.io.IOException;

public class Connect extends Command {

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
        return new String[]{"c", "connect"};
    }

}
