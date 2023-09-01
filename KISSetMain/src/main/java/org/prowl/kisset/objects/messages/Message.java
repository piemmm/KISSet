package org.prowl.kisset.objects.messages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.objects.InvalidMessageException;
import org.prowl.kisset.objects.Packetable;
import org.prowl.kisset.objects.Priority;
import org.prowl.kisset.util.Tools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Locale;

/**
 * A news message is from a user to a newsgroup
 */
public class Message extends Packetable {

    private static final Log LOG = LogFactory.getLog("Message");

    private long date;
    private String from;
    private String group = "";
    private String subject;
    private byte[] body;
    private String BID_MID;
    private String type;
    private String route;
    private long messageNumber; // Our internally assigned message number

    public Message() {
        // News messages are background work for nodes so are low priority.
        priority = Priority.LOW;
    }

    public String getBID_MID() {
        return BID_MID;
    }

    public void setBID_MID(String BID_MID) {
        this.BID_MID = BID_MID;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from.toUpperCase(Locale.ENGLISH);
    }

    public String getGroup() {
        return group;
    }

    // This is the group, or the to: field of the message
    public void setGroup(String group) {
        this.group = group.toUpperCase(Locale.ENGLISH);
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getMessageNumber() {
        return messageNumber;
    }

    public void setMessageNumber(long messageNumber) {
        this.messageNumber = messageNumber;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getTSLD() {
        return type; // for the moment just return the type
    }


    /**
     * Serialise into a byte array. Keeping the size to a minimum is important.
     * Length, data, length, data format for all the fields.
     *
     * @return A byte array representing the serialised message
     */
    public byte[] toPacket() {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(subject.length() + body.length + group.length() + 30);
             DataOutputStream dout = new DataOutputStream(bos)) {

            // String.length measures UTF units, which is no good to use, so we will use the
            // byte array size.
            byte[] groupArray = group.getBytes();
            byte[] fromArray = from.getBytes();
            byte[] subjectArray = subject.getBytes();
            byte[] bodyArray = body;
            byte[] bidmidArray = BID_MID.getBytes();
            byte[] typeArray = type.getBytes();
            byte[] routeArray = route.getBytes();

            // Start off with the message number
            dout.writeLong(messageNumber);

            // Start off with the date
            dout.writeLong(date);

            // Originating and latest paths
            toPacketPaths(dout);

            // Signed int, 4 bytes, easily handled by other systems.
            dout.writeInt(groupArray.length);
            dout.write(groupArray);

            dout.writeInt(fromArray.length);
            dout.write(fromArray);

            dout.writeInt(subjectArray.length);
            dout.write(subjectArray);

            dout.writeInt(bidmidArray.length);
            dout.write(bidmidArray);

            dout.writeInt(typeArray.length);
            dout.write(typeArray);

            dout.writeInt(routeArray.length);
            dout.write(routeArray);

            dout.writeInt(bodyArray.length);
            dout.write(bodyArray);

            dout.flush();
            dout.close();
            return bos.toByteArray();

        } catch (Throwable e) {
            LOG.error("Unable to serialise message", e);
        }
        return null;
    }

    /**
     * Deserialise from a byte array
     */
    public Message fromPacket(DataInputStream din) throws InvalidMessageException {

        try {
            long messageNumber = din.readLong();
            long date = din.readLong();

            fromPacketPaths(din);

            String group = Tools.readString(din, din.readInt());
            String from = Tools.readString(din, din.readInt());
            String subject = Tools.readString(din, din.readInt());
            String bidmid = Tools.readString(din, din.readInt());
            String type = Tools.readString(din, din.readInt());
            String route = Tools.readString(din, din.readInt());
            byte[] body = Tools.readBytes(din, din.readInt());

            setMessageNumber(messageNumber);
            setDate(date);
            setGroup(group);
            setFrom(from);
            setSubject(subject);
            setRoute(route);
            setBody(body);
            setBID_MID(bidmid);
            setType(type);

        } catch (Throwable e) {
            LOG.error("Unable to build message from packet", e);
            throw new InvalidMessageException(e.getMessage(), e);
        }
        return this;
    }


}
