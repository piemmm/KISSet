package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;

import java.io.IOException;

/**
 * Turn on/off monitoring of packets to the main terminal window instead of the dedicated monitor window
 */
@TNCCommand
public class Monitor  extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        if (data.length == 1) {
            writeToTerminal("Monitor is " + (tncHost.isMonitorEnabled() ? "on" : "off") + CR);
        } else {
            if (data[1].equalsIgnoreCase("on")) {
                tncHost.setMonitor(true);
                writeToTerminal("Monitor is on" + CR);
            } else if (data[1].equalsIgnoreCase("off")) {
                tncHost.setMonitor(false);
                writeToTerminal("Monitor is off" + CR);
            } else {
                writeToTerminal("*** Usage: mon [on|off]" + CR);
            }
        }
        return true;
    }



    @Override
    public String[] getCommandNames() {
        return new String[]{"mon", "m", "monitor"};
    }

}
