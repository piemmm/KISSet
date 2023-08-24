package org.prowl.kisset.protocols.xrouter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.core.Node;
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


    private String l3src;
    private String l3dst;
    private int l3ttl;

    // Text field
    private String fid;
    private int ts;
    private int srtt;
    private int rtt;
    private int pid;
    private String alias;
    private String id;
    private String version;
    private String maxt;
    private String flags;

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
        } catch (ParseException e) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("L3RTT:\r\n");
        sb.append(" l3src=");
        sb.append(l3src);
        sb.append("\r\n l3dst=");
        sb.append(l3dst);
        sb.append("\r\n l3ttl=");
        sb.append(l3ttl);
        sb.append("\r\n fid=");
        sb.append(fid);
        sb.append("\r\n ts=");
        sb.append(ts);
        sb.append("\r\n srtt=");
        sb.append(srtt);
        sb.append("\r\n rtt=");
        sb.append(rtt);
        sb.append("\r\n pid=");
        sb.append(pid);
        sb.append("\r\n alias=");
        sb.append(alias);
        sb.append("\r\n id=");
        sb.append(id);
        sb.append("\r\n version=");
        sb.append(version);
        sb.append("\r\n maxt=");
        sb.append(maxt);
        sb.append("\r\n flags=");
        sb.append(flags);
        return sb.toString();
    }

}
