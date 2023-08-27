package org.prowl.kisset.services.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.io.InterfaceStatus;
import org.prowl.kisset.io.StreamState;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;

/**
 * Change the current interface (like port on a kantronics)
 */
@TNCCommand
public class ChangeInterface extends Command {


    @Override
    public boolean doCommand(String[] data) throws IOException {

        // We're only interesteed in comamnd moed.
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        if (data.length > 1 && data[1].equals("?")) {
            writeToTerminal("*** Usage: interface <interface_number>" + CR);
            return true;
        }

        // No parameter? Just list the interfaces then
        if (data.length == 1) {
            showInterfaces();
            return true;
        }


        // Prevent interface changes whilst in connecting state
        if (commandParser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.CONNECTING)) {
            writeToTerminal("*** Cannot change interface whilst connecting");
            return true;
        }

        // If a stream number is specified, change to that stream.
        try {
            int interfaceNumber = Integer.parseInt(data[1]);
            if (interfaceNumber < 0 || interfaceNumber > KISSet.INSTANCE.getInterfaceHandler().getInterfaces().size() - 1) {
                writeToTerminal("*** Invalid stream number");
                return true;
            }

            // Change the interfaces
            commandParser.setCurrentInterface(KISSet.INSTANCE.getInterfaceHandler().getInterface(interfaceNumber));

            // Change the stream (and output stream) to stream 0
            commandParser.getCurrentInterface().setCurrentStream(0);
            commandParser.setDivertStream(commandParser.getCurrentInterface().getCurrentStream().getOutputStream());

            writeToTerminal("*** Changed to interface " + interfaceNumber + CR);

            return true;
        } catch (NumberFormatException e) {
            writeToTerminal("*** Invalid stream number");
            return true;
        }
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"interface", "int", "port"};
    }


    public void showInterfaces() throws IOException {
        writeToTerminal(CR + ANSI.BOLD + ANSI.UNDERLINE + "No. State Interface                                      " + ANSI.NORMAL + CR);
        int i = 0;
        for (Interface anInterface : KISSet.INSTANCE.getInterfaceHandler().getInterfaces()) {
            InterfaceStatus interfaceStatus = anInterface.getInterfaceStatus();
            String statusCol;
            if (interfaceStatus.getState() == InterfaceStatus.State.OK) {
                statusCol = ANSI.GREEN;
            } else if (interfaceStatus.getState() == InterfaceStatus.State.WARN) {
                statusCol = ANSI.YELLOW;
            } else if (interfaceStatus.getState() == InterfaceStatus.State.ERROR) {
                statusCol = ANSI.RED;
            } else {
                statusCol = ANSI.WHITE;
            }
            writeToTerminal(StringUtils.rightPad(i + ": ", 4) + statusCol + StringUtils.rightPad(interfaceStatus.getState().name(), 6) + ANSI.NORMAL + anInterface + CR);
            if (interfaceStatus.getMessage() != null) {
                writeToTerminal("      " + statusCol + "\\-" + interfaceStatus.getMessage() + ANSI.NORMAL + CR);
            }
            i++;
        }
        writeToTerminal(CR);
    }
}
