package org.prowl.kisset.services.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.objects.messages.Message;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

@TNCCommand
/**
 * List message left in the mail system from other stations
 */
public class ListMessages extends Command {

    private int listMessagesStartingPoint = 0;                                   // Used for list messages command
    private java.util.List<Message> currentListMessages;                               // Used for list messages command


    @Override
    public boolean doCommand(String[] data) throws IOException {

        Mode mode = getMode();
        if (mode.equals(Mode.CMD) && data[0].length() > 0) {
            listMessages();
            return true;
        } else if (mode.equals(Mode.MESSAGE_LIST_PAGINATION) && data[0].length() == 0) {
            // if we are paginating, and enter was pressed (0 length data[0]), then send the next page
            sendMessageList(currentListMessages);
            return true;
        }

        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        if (data.length > 1 && data[1].equals("?")) {
            writeToTerminal("*** Usage: list" + CR);
            return true;
        }

        return false;
    }

    public void listMessages() throws IOException {
        listMessagesStartingPoint = 0;
        Storage storage = KISSet.INSTANCE.getStorage();
        java.util.List<Message> messages = storage.getMessagesInOrder(null);
        if (messages.size() == 0) {
            writeToTerminal(CR);
            writeToTerminal("No messages in PMS yet" + CR);
        } else {
            writeToTerminal(CR);
            writeToTerminal(ANSI.UNDERLINE + ANSI.BOLD + "Msg#   TSLD  Size To     @Route  From    Date/Time Subject" + ANSI.NORMAL + CR);
            currentListMessages = messages; // Store filtered list for pagination sending
            sendMessageList(currentListMessages);
        }
    }

    public void sendMessageList(java.util.List<Message> messages) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMM/hhmm");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        nf.setGroupingUsed(false);

        int messageSentCounter = 0;
        if (listMessagesStartingPoint != 0) {
            //    write(CR);
        }
        for (int i = listMessagesStartingPoint; i < messages.size(); i++) {
            Message message = messages.get(i);

            writeToTerminal((StringUtils.rightPad(nf.format(message.getMessageNumber()), 6) + // MessageId
                    StringUtils.rightPad("", 6) +  // TSLD
                    StringUtils.leftPad(nf.format(message.getBody().length), 5) + " " + // Size
                    StringUtils.rightPad(message.getGroup(), 7) +  // To
                    StringUtils.rightPad(message.getRoute(), 8) + // @route
                    StringUtils.rightPad(message.getFrom(), 8) + // from
                    StringUtils.rightPad(sdf.format(message.getDate()), 10) + // date/time
                    StringUtils.rightPad(message.getSubject(), 50)).trim() + CR);

            if (++messageSentCounter >= 22) { // todo '10' should be configurable by the user
                setMode(Mode.MESSAGE_LIST_PAGINATION);
                listMessagesStartingPoint += messageSentCounter;
                break;
            }
        }
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"listmessages", "list", "l"};
    }
}
