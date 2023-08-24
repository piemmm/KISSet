package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.gui.terminals.ANSITerminal;
import org.prowl.kisset.gui.terminals.TeletextTerminal;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;

/**
 * Sets the terminal type to whatever terminal type is chosen by the user
 *
 * eg:  TERM <terminal type>
 *      TERM teletext - for teletext/prestel/SAA5050 compatibility
 *
 *      TERM ansi - this is the default terminal type
 *      TERM plain/ascii - for plain text, strips any nonprintable characters
 *      TERM debug - useful for debugging the terminal by showing control characters
 *      TERM auto - for automatically switching to the best terminal type for things like telstar/prestel/etc
 *
 */
@TNCCommand
public class Term extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        // If no terminal type is specified, show the current terminal type
        if (data.length == 1) {
            writeToTerminal("Terminal type: " + tncHost.getTerminalType().getName() + CR);
            return true;
        }

        // If a terminal type is specified, set it
        String termType = data[1].toLowerCase();
        switch (termType) {
            case "teletext":
                tncHost.setTerminalType(new TeletextTerminal());
                break;
            case "ansi":
                tncHost.setTerminalType(new ANSITerminal());
                break;
//            case "plain":
//            case "ascii":
//                tncHost.setTerminalType(new ASCIITerminal());
//                break;
//            case "debug":
//                tncHost.setTerminalType(new DebugTerminal());
//                break;
//            case "auto":
//                tncHost.setTerminalType(new AutoTerminal());
//                break;
            default:
                writeToTerminal("Unknown terminal type: " + termType + CR);
                break;
        }

        writeToTerminal("Terminal changed to " + tncHost.getTerminalType().getName() + CR);

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"term", "terminal"};
    }
}
