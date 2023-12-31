package org.prowl.kisset.services.remote.pms.parser.commands;

import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.PMSCommand;
import org.prowl.kisset.services.remote.pms.parser.Mode;

import java.io.IOException;

/**
 * Help for commands in CMD mode only
 */
@PMSCommand
public class Help extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        write(CR);
        write(Messages.get("pms_help") + CR);
        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"help", "?", "h"};
    }
}
