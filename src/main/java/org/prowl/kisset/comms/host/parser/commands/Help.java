package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;

import java.io.IOException;

/**
 * Help for commands in CMD mode only
 */
@TNCCommand
public class Help extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        write(CR);
        write(Messages.get("help") + CR);
        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"help", "?", "h"};
    }
}
