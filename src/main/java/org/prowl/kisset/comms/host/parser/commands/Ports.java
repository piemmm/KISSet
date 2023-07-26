package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;

/**
 * Ports on a TNC - these are various interfaces that the user may have conneted to in the app and this command
 * allows you to list them
 */
@TNCCommand
public class Ports extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        write(ANSI.UNDERLINE + ANSI.BOLD + "Port   Driver" + ANSI.NORMAL + CR);

        int i = 0;
        for (Interface anInterface : KISSet.INSTANCE.getInterfaceHandler().getInterfaces()) {
            write(String.format("%-6s %s", i, anInterface.toString()) + CR);
            i++;
        }

        write(CR);

        return true;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"p", "ports"};
    }

}
