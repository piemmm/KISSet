package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;

import java.io.IOException;

/**
 * Display a list of settings from KISS and also some app preferences.
 */
@TNCCommand
public class Disp extends Command {


    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        // Show a list of the settings available (this loosely mirrors the command help as well)

        writeToTerminal("MYcall: " + KISSet.INSTANCE.getMyCall() + CR);
        writeToTerminal("MONitor: " + (tncHost.isMonitorEnabled() ? "ON":"OFF") + CR);

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"disp", "display"};
    }

}
