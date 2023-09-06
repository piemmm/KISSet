package org.prowl.kisset.services.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.ax25.KissParameter;
import org.prowl.ax25.KissParameterType;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.services.host.parser.CommandParser;
import org.prowl.kisset.services.host.parser.Mode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Change the terminal type - there are 2 types of terminals, one for tty mode and one set for gui mode
 */
@TNCCommand
public class Term extends Command {

    public static Map<String, String[]> terminalTypes = new HashMap<>();

    static {
        terminalTypes.put("ansi", new String[]{"org.prowl.kisset.userinterface.stdinout.StdANSI", "org.prowl.kissetgui.userinterface.desktop.terminals.ANSITerminal"});
        terminalTypes.put("teletext", new String[]{"org.prowl.kisset.userinterface.stdinout.StdTeletext", "org.prowl.kissetgui.userinterface.desktop.terminals.TeletextTerminal"});
        terminalTypes.put("debug", new String[]{"org.prowl.kisset.userinterface.stdinout.StdDebug", "org.prowl.kissetgui.userinterface.desktop.terminals.DebugTerminal"});
    }

    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        // Show our current type
        if (data.length == 1) {
            String type = tncHost.getTerminalType().getClass().getName();
            for (Map.Entry<String, String[]> entry : terminalTypes.entrySet()) {
                if (entry.getValue()[0].equals(type) || entry.getValue()[1].equals(type)) {
                    writeToTerminal("Current terminal type: " + entry.getKey() + CR);
                    return true;
                }
            }
            writeToTerminal("Unknown terminal type:" + tncHost.getTerminalType() + CR);
            return true;
        }

        // Change to a new type.
        if (data.length == 2) {
            String type = data[1].toLowerCase();
            if (terminalTypes.containsKey(type)) {
                try {
                    String className;
                    if (KISSet.INSTANCE.isTerminalMode()) {
                        // TTY mode
                        className = terminalTypes.get(type)[0];
                    } else {
                        // GUI mode
                        className  = terminalTypes.get(type)[1];
                    }
                    Class<?> clazz = Class.forName(className);
                    tncHost.setTerminalType(clazz.newInstance());
                    writeToTerminal("*** Terminal type changed to " + type + CR);
                    return true;
                } catch (Throwable e) {
                    writeToTerminal("*** Failed to change terminal type to " + type + ": " + e.getMessage() + CR);
                    return true;
                }
            }
            writeToTerminal("Unknown terminal type: " + type + CR);
            return true;
        }

        return true;
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"term", "termtype"};
    }




}
