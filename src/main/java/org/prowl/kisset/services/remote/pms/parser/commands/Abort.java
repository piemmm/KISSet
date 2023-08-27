package org.prowl.kisset.services.remote.pms.parser.commands;


import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.PMSCommand;
import org.prowl.kisset.services.remote.pms.parser.Mode;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;

@PMSCommand
public class Abort extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {


        if (getMode().equals(Mode.MESSAGE_LIST_PAGINATION) || getMode().equals(Mode.MESSAGE_READ_PAGINATION)) {
            write(ANSI.BOLD + Messages.get("abortMessageList") + ANSI.NORMAL + CR);
            popModeFromStack();
            return true;
        }
        return false;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"abort", "a"};
    }
}
