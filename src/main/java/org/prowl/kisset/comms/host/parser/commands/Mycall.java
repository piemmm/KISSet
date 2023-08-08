package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.config.Conf;

import java.io.IOException;

/**
 * Display or set the station callsign
 */
@TNCCommand
public class Mycall extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        if (data.length == 1) {
            writeToTerminal("*** MYcall is " + KISSet.INSTANCE.getMyCall() + CR);
        } else {
            String call = data[1].toUpperCase();
            KISSet.INSTANCE.setMyCall(call);
            writeToTerminal("*** MYcall set to " + KISSet.INSTANCE.getMyCall() + CR);
            KISSet.INSTANCE.getConfig().setProperty(Conf.callsign,call).saveConfig();


        }

        return true;
    }



    @Override
    public String[] getCommandNames() {
        return new String[]{"my", "mycall"};
    }

}
