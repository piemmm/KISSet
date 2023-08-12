package org.prowl.kisset.comms.remote.pms.parser.commands;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.PMSCommand;
import org.prowl.kisset.comms.remote.pms.parser.Mode;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.objects.messages.Message;
import org.prowl.kisset.util.PacketTools;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Reads a message on the PMS
 */
@PMSCommand
public class ReadMessage extends Command {

    // So we can paginate.
    private int readMessageStartingPoint = 0;

    // The current message being read
    private Message currentMessage;

    // A 'buffer' of lines we can paginate on.
    private List<String> messageLines;


    @Override
    public boolean doCommand(String[] data) throws IOException {
        Mode mode = getMode();
        if ((mode.equals(Mode.CMD) || mode.equals(Mode.MESSAGE_LIST_PAGINATION)) && data[0].equals("r")) {
            pushModeToStack(mode);
            try {
                readMessage(Long.parseLong(data[1]));
            } catch (NumberFormatException e) {
                write("Invalid message number" + CR);
            }
            return true;
        } else if (mode.equals(Mode.MESSAGE_READ_PAGINATION)) {
            sendMessage();
            return true;
        }
        return false;
    }


    public void readMessage(long messageId) throws IOException {
        Storage storage = KISSet.INSTANCE.getStorage();
        Message message = storage.getMessage(messageId);
        readMessageStartingPoint = 0;
        messageLines = new ArrayList<>();

        // Get the message and write it to the messageLines array so we can later paginate on it correctly.
        if (message == null) {
            write("Message not found" + CR);
        } else {
            currentMessage = message;
            messageLines.add("From: " + message.getFrom());
            messageLines.add("To: " + message.getGroup());
            messageLines.add("Subject: " + message.getSubject());
            messageLines.add("Date: " + message.getDate());
            messageLines.add("Route: " + message.getRoute());
            messageLines.add("TSLD: " + message.getTSLD());
            messageLines.add("Size: " + message.getBody().length);
            messageLines.add("");
            StringTokenizer st = new StringTokenizer(PacketTools.textOnly(message.getBody()), "\n\r");
            while (st.hasMoreTokens()) {
                messageLines.add(st.nextToken());
            }
            sendMessage();
        }
    }

    public void sendMessage() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMM/hhmm");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        nf.setGroupingUsed(false);

        int messageSentCounter = 0;
        for (int i = readMessageStartingPoint; i < messageLines.size(); i++) {

            write(messageLines.get(i) + CR);

            if (++messageSentCounter >= 22) { // todo '10' should be configurable by the user
                setMode(Mode.MESSAGE_READ_PAGINATION);
                readMessageStartingPoint += messageSentCounter;
                return;
            }
        }
        // Reading done, return to previous mode.
        popModeFromStack();
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"read", "r", ""};
    }
}
