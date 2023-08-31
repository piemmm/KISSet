package org.prowl.kisset.services.host.parser.commands;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;

/**
 * Display a list of settings from KISS and also some app preferences.
 */
@TNCCommand
public class Quit extends Command {


    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        // Show a list of the settings available (this loosely mirrors the command help as well)
        writeToTerminal(ANSI.YELLOW+ANSI.BOLD+"*** Quit requested, shutting down: "+ANSI.NORMAL+ CR);

        KISSet.INSTANCE.quit();

        // We're probably not getting here given the above calls System.exit()
        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"quit", "exit"};
    }

}
