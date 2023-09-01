package org.prowl.kisset.objects;

import org.prowl.kisset.util.Tools;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * An item we can packetise.
 * <p>
 * packets are usually of the format:
 * <p>
 * date:length:segment:length:segment:length:segment..... where length is the
 * length in bytes of the string(segment). Date is a long.
 */
public abstract class Packetable {

    protected Priority priority;             // The priority of this message

    protected String originatingPath = ""; // The originating path of this message (once filled never overwritten), ~64
    // chars

    protected String latestPath = ""; // The latest few nodes this packet has passed through (latest at the end,
    // oldest at the start, fixed size), ~64 chars

    public abstract byte[] toPacket();

    public abstract Packetable fromPacket(DataInputStream din) throws InvalidMessageException;

    /**
     * Add to the originating and latest paths that this packet has travelled.
     *
     * @param nodeId The node id. Should never contain ':' and always be less than
     *               15 characters.
     */
    public void addToPath(String nodeId) {

        nodeId = nodeId.replace(":", "?").toUpperCase(Locale.ENGLISH); // : not allowed, stop potential spoof.
        if (nodeId.length() > 15) {
            nodeId = nodeId.substring(0, 15) + "...";
        }

        if (originatingPath.length() < 64) {
            originatingPath = originatingPath + ":" + nodeId;
        }
        if (originatingPath.length() > 64) {
            originatingPath = originatingPath.substring(0, 64);
        }

        latestPath = latestPath + ":" + nodeId;
        if (latestPath.length() > 64) {
            latestPath = "..." + latestPath.substring(latestPath.length() - 64);
        }

    }

    /**
     * Serialise the originator and latest paths
     *
     * @return
     * @throws IOException
     */
    protected void toPacketPaths(DataOutputStream dos) throws IOException {

        byte[] originatingArray = originatingPath.getBytes();
        byte[] latestArray = latestPath.getBytes();

        dos.writeInt(originatingArray.length);
        dos.write(originatingArray);

        dos.writeInt(latestArray.length);
        dos.write(latestArray);

    }

    protected void fromPacketPaths(DataInputStream din) throws IOException {

        String originating = Tools.readString(din, din.readInt());
        String latest = Tools.readString(din, din.readInt());

        originatingPath = originating.toUpperCase(Locale.ENGLISH);
        latestPath = latest.toUpperCase(Locale.ENGLISH);

    }

    public String getOriginatingPath() {
        return originatingPath;
    }

    public String getLatestPath() {
        return latestPath;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
}
