package org.prowl.kisset.services.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.objects.Priority;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.objects.messages.Message;
import org.prowl.kisset.util.Tools;

import java.io.IOException;

/**
 * Let the user write a message to be stored in the PMS system - the message can be either private or a bulletin.
 */
@TNCCommand
public class SendMessage extends Command {
    private static final Log LOG = LogFactory.getLog("SendMessage");

    // The current state of the message builder.
    private MessageState messageState;

    private Message buildingMessage;
    private StringBuilder messageBody;

    @Override
    public boolean doCommand(String[] data) throws IOException {



        Mode mode = getMode();
        LOG.debug("buildMessage:" + mode + ":" + messageState + ":" + data[0]);

        if (mode.equals(Mode.CMD) && data[0].length() > 0) {
            if (data[0].equals("sb") || data[0].equals("sendb")) {
                // Put into send message mode
                messageState = null;
                setMode(Mode.SENDING_PUBLIC_MESSAGE);
                buildMessage(data);
                return true;
            } else if (data[0].equals("sp") || data[0].equals("sendp")) {
                // Put into send message mode
                messageState = null;
                setMode(Mode.SENDING_PRIVATE_MESSAGE);
                buildMessage(data);
                return true;
            } else {
                return false; // Not for us.
            }

        } else if (mode.equals(Mode.SENDING_PUBLIC_MESSAGE) || mode.equals(Mode.SENDING_PRIVATE_MESSAGE)) {
            buildMessage(data);
            return true;
        }


        return false;
    }

    /**
     * Build the message from the data provided.
     *
     * @param data The data to build the message from.
     */
    public void buildMessage(String[] data) throws IOException {
        Mode mode = getMode();

        // The message states - last state is first so it is easy for us to error to a previous state if the user enters
        // an invalid value.
        if (MessageState.GET_CONFIRMATION.equals(messageState)) {
            String confirmation = data[0];
            if (confirmation.equalsIgnoreCase("y")) {
                messageState = null;
                Storage storage = KISSet.INSTANCE.getStorage();
                long messageId = storage.getNextMessageID();
                buildingMessage.setBID_MID("" + messageId);
                buildingMessage.setMessageNumber(messageId);
                buildingMessage.setBody(messageBody.toString().getBytes());
                buildingMessage.setType("P");
                buildingMessage.setFrom(KISSet.INSTANCE.getMyCall());
                buildingMessage.setRoute(KISSet.INSTANCE.getMyCall());
                buildingMessage.setPriority(Priority.LOW);
                buildingMessage.setDate(System.currentTimeMillis());
                storage.storeNewsMessage(buildingMessage);
                messageState = null;
                messageBody = new StringBuilder();
                buildingMessage = null;
                writeToTerminal("*** Message saved as "+messageId + CR);
                setMode(Mode.CMD);
                return;
            } else if (confirmation.equalsIgnoreCase("n")) {
                // Cancel the message
                writeToTerminal("Message cancelled" + CR);
                messageState = null;
                messageBody = new StringBuilder();
                buildingMessage = null;
                setMode(Mode.CMD);
                return;
            } else {
                messageState = MessageState.GET_CONFIRMATION;
                writeToTerminal("*** Invalid confirmation" + CR);
                writeToTerminal("Do you wish to send this message? (y/n): ");
            }
        }

        if (MessageState.GET_MESSAGE.equals(messageState)) {
            // Validate the entered message
            if (data[0].equals("/ex") || data[0].equals(".")) {
                writeToTerminal("Do you wish to send this message? (y/n): ");
                messageState = MessageState.GET_CONFIRMATION;
            } else {
                String message = StringUtils.join(data, " ");
                messageBody.append(message + CR);
            }
        }

        if (MessageState.GET_SUBJECT.equals(messageState)) {
            // Validate the entered subject
            String subject = data[0];
            if (Tools.isAlphaNumeric(subject)) {
                buildingMessage.setSubject(subject);
                messageState = MessageState.GET_MESSAGE;
                writeToTerminal("Enter the message body, end with a '/ex' or a '.' on a line by itself:"+CR);
            } else {
                messageState = MessageState.GET_SUBJECT;
                writeToTerminal("*** Invalid subject"+CR);
            }
        }

        if (MessageState.START.equals(messageState)) {
            // Validate the entered callsign
            String callsign = data[0];
            if ((mode.equals(Mode.SENDING_PUBLIC_MESSAGE) && Tools.isAlphaNumeric(callsign) ||
                    (mode.equals(Mode.SENDING_PRIVATE_MESSAGE) && Tools.isValidITUCallsign(callsign)))) {
                buildingMessage.setGroup(callsign);
                messageState = MessageState.GET_SUBJECT;
                writeToTerminal("Enter the subject of the message: ");
            } else {
                messageState = null;
                writeToTerminal("*** Invalid callsign"+CR);
            }
        }

        if (messageState == null) {
            buildingMessage = new Message();
            messageState = MessageState.START;
            messageBody = new StringBuilder();
            writeToTerminal("*** Abort message creation by typing '/abort' at any time"+CR);
            writeToTerminal("Enter the callsign of the station you wish to send the message to: ");
        }
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"sb", "sp", "sendb", "sendp"};
    }


    private enum MessageState {
        START,
        GET_TO,
        GET_SUBJECT,
        GET_MESSAGE,
        GET_CONFIRMATION,
        SEND_MESSAGE,
        SEND_MESSAGE_CONFIRMED,
        SEND_MESSAGE_FAILED
    }

    ;
}
