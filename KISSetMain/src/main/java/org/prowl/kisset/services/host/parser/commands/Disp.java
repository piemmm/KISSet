package org.prowl.kisset.services.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.ax25.KissParameter;
import org.prowl.ax25.KissParameterType;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.services.host.parser.CommandParser;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.ANSI;

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
        writeToTerminal(ANSI.BOLD + ANSI.UNDERLINE + "Global settings:" + ANSI.NORMAL + CR);
        showValue(this, "MYcall", KISSet.INSTANCE.getMyCall());
        showValue(this, "MONitor", (tncHost.isMonitorEnabled() ? "ON" : "OFF"));

        // Interface specific settings
        writeToTerminal(CR + ANSI.BOLD + ANSI.UNDERLINE + "Current interface settings:" + ANSI.NORMAL + CR);
        Interface currentInterface = commandParser.getCurrentInterface();
        if (currentInterface == null) {
            writeToTerminal("No interface selected" + CR);
            return true;
        }
        showValue(this, "Interface is", currentInterface.getInterfaceStatus().getState().name());
        showValue(this, "TXDelay", getKissParameterValue(commandParser, KissParameterType.TXDELAY));
        showValue(this, "PERsistence", getKissParameterValue(commandParser, KissParameterType.PERSISTENCE));
        showValue(this, "SLOTtime", getKissParameterValue(commandParser, KissParameterType.SLOT_TIME));
        showValue(this, "TXTail", getKissParameterValue(commandParser, KissParameterType.TX_TAIL));
        showValue(this, "FULLDuplex", getKissParameterValue(commandParser, KissParameterType.FULL_DUPLEX));
        showValue(this, "SETHardware", getKissParameterValue(commandParser, KissParameterType.SET_HARDWARE));
        return true;
    }

    public static void showValue(Command command, String name, String value) throws IOException {
        command.writeToTerminal(StringUtils.rightPad(name, 16) + ": " + value + CR);
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"disp", "display"};
    }

    /**
     * Get the kiss parameter value as a human reasable string, taking into account any n*10 conversions.
     *
     * @param parameterType
     * @return
     */
    public static String getKissParameterValue(CommandParser commandParser, KissParameterType parameterType) {
        KissParameter kissParameter = commandParser.getCurrentInterface().getKissParameter(parameterType);
        if (kissParameter == null) {
            return "N/A";
        }
        int[] value = kissParameter.getData();


        if (parameterType == KissParameterType.TXDELAY || parameterType == KissParameterType.TX_TAIL || parameterType == KissParameterType.SLOT_TIME) {
            return value[0] + " (" + String.valueOf(value[0] * 10) + "ms)";
        } else if (parameterType == KissParameterType.FULL_DUPLEX) {
            return (value[0] == 0 ? "OFF" : "ON");
        } else if (parameterType == KissParameterType.SET_HARDWARE) {
            // Unknown!
            return String.valueOf(value);
        }

        return String.valueOf(value[0]);
    }


}
