package org.prowl.kisset.services.remote.netrom.user.parser.commands;

import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.NodeCommand;
import org.prowl.kisset.services.remote.netrom.user.parser.Mode;


import java.io.IOException;

/**
 * Help for commands in CMD mode only
 */
@NodeCommand
public class Help extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        write(CR);
        write(Messages.get("node_help") + CR);
        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"help", "?", "h"};
    }
}
