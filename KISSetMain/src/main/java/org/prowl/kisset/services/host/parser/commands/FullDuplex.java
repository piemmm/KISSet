package org.prowl.kisset.services.host.parser.commands;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.ax25.KissParameterType;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.io.IOException;

/**
 * Set the Full duplex KISS parameter
 */
@TNCCommand
public class FullDuplex extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        // This command requires CMD mode to be active
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        // Check the user has configured and selected an interface
        if (commandParser.getCurrentInterface() == null) {
            writeToTerminal(ANSI.RED + "*** No interface selected" + ANSI.NORMAL + CR);
            return true;
        }

        // Querying the value or setting it?
        if (data.length == 1) {
            Disp.showValue(this, "FULLDuplex", Disp.getKissParameterValue(commandParser, KissParameterType.FULL_DUPLEX));
        } else {

            // Get the value and check it
            boolean value =false;
            try {
               value = Boolean.parseBoolean(data[1]);
            } catch (Exception e) {
                writeToTerminal(ANSI.RED + "*** Invalid value" + ANSI.NORMAL + CR);
            }

            // Set it on the interface
            commandParser.getCurrentInterface().setKissParameter(KissParameterType.FULL_DUPLEX, value ? 1 : 0);

            // Now update the config.
            Config config = KISSet.INSTANCE.getConfig();
            HierarchicalConfiguration iConfig = config.getInterfaceConfig(commandParser.getCurrentInterface().getUUID());
            iConfig.setProperty(Conf.fullDuplex.name(), value);
            config.saveConfig();

            // Show the new value to the user
            Disp.showValue(this, "FULLDuplex", Disp.getKissParameterValue(commandParser, KissParameterType.FULL_DUPLEX));
        }

        return true;
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"fulld", "fullduplex"};
    }

}
