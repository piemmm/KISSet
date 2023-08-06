package org.prowl.kisset.comms.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.io.Stream;
import org.prowl.kisset.io.StreamState;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;

/**
 * Change the current interface (like port on a kantronics)
 */
@TNCCommand
public class ChangeInterface extends Command {


    @Override
    public boolean doCommand(String[] data) throws IOException {

        if (data.length > 1 && data[1].equals("?")) {
            writeToTerminal("*** Usage: interface <interface_number>" + CR);
            return true;
        }

        // No parameter? Just list the interfaces then
        if (data.length == 1) {
            writeToTerminal(CR+ANSI.BOLD+ANSI.UNDERLINE+"No. Interface                                      "+ANSI.NORMAL + CR);
            int i = 0;
            for (Interface anInterface : KISSet.INSTANCE.getInterfaceHandler().getInterfaces()) {
                String status = anInterface.getFailReason();
                if (status == null) {
                    status = "OK";
                }
                writeToTerminal(StringUtils.rightPad(Integer.toString(i)+": ",4)+ StringUtils.rightPad(anInterface.toString(),25) + StringUtils.rightPad(status,30) + CR);
                i++;
            }
            writeToTerminal(CR);
            return true;
        }


        // Prevent interface changes whilst in connecting state
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTING)) {
            writeToTerminal("*** Cannot change interface whilst connecting");
            return false;
        }

        return false;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"interface", "int", "port"};
    }


}
