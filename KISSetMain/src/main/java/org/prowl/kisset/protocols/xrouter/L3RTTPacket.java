package org.prowl.kisset.protocols.xrouter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.protocols.core.Node;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.StringTokenizer;

/**
 * This represents an INP3 L3RTT packet
 */
public class L3RTTPacket {

    private static final Log LOG = LogFactory.getLog("L3RTTPacket");


    private final String l3src;
    private final String l3dst;
    private int l3ttl;

    // Text field
    private final String fid;
    private final int ts;
    private final int srtt;
    private final int rtt;
    private final int pid;
    private final String alias;
    private final String id;
    private final String version;
    private final String maxt;
    private final String flags;

    /**
     * Decode an INP3 L3RTT packet including the callsign from the body of the packet
     * <p>
     * These packets aren't efficiently encoded.
     *
     * @param node
     */
    public L3RTTPacket(Node node) throws ParseException {

        ByteBuffer buffer = ByteBuffer.wrap(node.getFrame().getBody());
        // Src and dest callsigns
        l3src = PacketTools.getData(buffer, 7, true);
        l3dst = PacketTools.getData(buffer, 7, true);
        l3ttl = buffer.get() & 0xFF;

        // Dummy L4 header
        int z1 = buffer.get() & 0xFF; // Skip the 0x00 byte
        int z2 = buffer.get() & 0xFF; // Skip the 0x00 byte
        int z3 = buffer.get() & 0xFF; // Skip the 0x00 byte
        int z4 = buffer.get() & 0xFF; // Skip the 0x00 byte
        int b1 = buffer.get() & 0xFF; // Skip the 0x05 byte


        String textPortion = new String(buffer.array(), buffer.position(), buffer.remaining());
        StringTokenizer st = new StringTokenizer(textPortion, " ");

        // Now decode the payload
        fid = st.nextToken();
        ts = Integer.parseInt(st.nextToken());
        srtt = Integer.parseInt(st.nextToken());
        rtt = Integer.parseInt(st.nextToken());
        pid = Integer.parseInt(st.nextToken());
        alias = st.nextToken();
        id = st.nextToken();
        version = st.nextToken();
        maxt = st.nextToken();
        flags = st.nextToken();

        l3ttl--;

    }

    /**
     * Is this a valid L3RTT packet?
     *
     * @param node
     * @return
     */
    public static boolean isL3RTT(Node node) {

        ByteBuffer buffer = ByteBuffer.wrap(node.getFrame().getBody());
        // Src and dest callsigns
        try {
            String senderNodeCall = PacketTools.getData(buffer, 7, true);
            String l3rttCall = PacketTools.getData(buffer, 7, true); // Always L3RTT-0
        } catch (Throwable e) {
            return false;
        }
        int ttl = buffer.get() & 0xFF;

        // Dummy L4 header
        int z1 = buffer.get() & 0xFF; // Skip the 0x00 byte
        int z2 = buffer.get() & 0xFF; // Skip the 0x00 byte
        int z3 = buffer.get() & 0xFF; // Skip the 0x00 byte
        int z4 = buffer.get() & 0xFF; // Skip the 0x00 byte
        int b1 = buffer.get() & 0xFF; // Skip the 0x05 byte

        // If it just stops then it's not an L3RTT packet.
        if (!buffer.hasRemaining()) {
            return false;
        }

        try {
            String textPortion = new String(buffer.array(), buffer.position(), buffer.remaining());
            StringTokenizer st = new StringTokenizer(textPortion, " ");

            // Now decode the payload
            String fid = st.nextToken();

            if (z1 == 0 && z2 == 0 && z3 == 0 && z4 == 0 && b1 == 5 && fid.equals("L3RTT:")) {
                return true;
            }
        } catch (Throwable e) {
            LOG.debug("Invalid L3RTT packet:" + Tools.byteArrayToReadableASCIIString(node.getFrame().getBody()), e);
        }
        return false;
    }

    public String getL3src() {
        return l3src;
    }

    public String getL3dst() {
        return l3dst;
    }

    /**
     * Get the TTL - it is decremented upon reception of this packet
     *
     * @return
     */
    public int getL3ttl() {
        return l3ttl;
    }

    public String getFid() {
        return fid;
    }

    public int getTs() {
        return ts;
    }

    public int getSrtt() {
        return srtt;
    }

    public int getRtt() {
        return rtt;
    }

    public int getPid() {
        return pid;
    }

    public String getAlias() {
        return alias;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getMaxt() {
        return maxt;
    }

    public String getFlags() {
        return flags;
    }

    public String toString() {
        String sb = "L3RTT:\r\n" +
                " l3src=" +
                l3src +
                "\r\n l3dst=" +
                l3dst +
                "\r\n l3ttl=" +
                l3ttl +
                "\r\n fid=" +
                fid +
                "\r\n ts=" +
                ts +
                "\r\n srtt=" +
                srtt +
                "\r\n rtt=" +
                rtt +
                "\r\n pid=" +
                pid +
                "\r\n alias=" +
                alias +
                "\r\n id=" +
                id +
                "\r\n version=" +
                version +
                "\r\n maxt=" +
                maxt +
                "\r\n flags=" +
                flags;
        return sb;
    }

}
