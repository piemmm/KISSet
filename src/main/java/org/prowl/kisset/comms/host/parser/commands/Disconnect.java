package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;

import java.io.IOException;

@TNCCommand
public class Disconnect extends Command {

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
        return new String[]{"disc", "d", "disconnect"};
    }

}
