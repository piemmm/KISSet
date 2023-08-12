package org.prowl.kisset.comms.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.objects.messages.Message;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

@TNCCommand
/**
 * Delete a message from the PMS system.
 */
public class DeleteMessage extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {
        Mode mode = getMode();
        if (!mode.equals(Mode.CMD) ) {
            return false;
        }

        if (data.length > 1 && data[1].equals("?")) {
            writeToTerminal("*** Usage: kill <message number>" + CR);
            return true;
        }
        try {
            int messageId = Integer.parseInt(data[1]);
            Message message = KISSet.INSTANCE.getStorage().getMessage(messageId);
            if (message == null) {
                writeToTerminal("*** Message not found" + CR);
                return true;
            } else {
                KISSet.INSTANCE.getStorage().deleteMessage(messageId);
                writeToTerminal("*** Message deleted" + CR);
                return true;
            }
        } catch(NumberFormatException e) {
            writeToTerminal("*** Invalid message number" + CR);
            return true;
        }


    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"km", "kill"};
    }
}
