package org.prowl.kisset.services.remote.netrom.user.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.NodeCommand;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.io.InterfaceStatus;
import org.prowl.kisset.services.remote.netrom.user.parser.Mode;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;
import java.text.NumberFormat;

@NodeCommand
public class Interfaces extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        // We're only interesteed in comamnd moed.
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        write(CR);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        nf.setMinimumFractionDigits(3);

        NumberFormat nfb = NumberFormat.getInstance();
        nfb.setMaximumFractionDigits(1);
        nfb.setMinimumFractionDigits(1);

        // No parameter? Just list the interfaces then
        if (data.length == 1) {
            write(CR + ANSI.BOLD + ANSI.UNDERLINE + "No. Interface                                      " + ANSI.NORMAL + CR);
            int i = 0;
            for (Interface anInterface : KISSet.INSTANCE.getInterfaceHandler().getInterfaces()) {
                InterfaceStatus status = anInterface.getInterfaceStatus();


                write(StringUtils.rightPad(Integer.toString(i) + ": ", 4) + StringUtils.rightPad(anInterface.toString(), 25) + StringUtils.rightPad(status.getState().name(), 30) + CR);
                i++;
            }
            write(CR);
            return true;
        }
        return true;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"int", "ports", "i", "interfaces"};
    }
}
