package org.prowl.kisset.netrom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.ax25.AX25Frame;
import org.prowl.kisset.core.Node;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes a Net/ROM routing broadcast packet from an AX.25 frame
 */
public class NetROMRoutingPacket {
    private static final Log LOG = LogFactory.getLog("NetROMRoutingPacket");

    private String sendingNode;

    private List<NetROMNode> nodesInThisPacket = new ArrayList<>();

    /**
     * Parse a netrom routing pocket.
     * @param node
     */
    public NetROMRoutingPacket(Node node) {

        ByteBuffer buffer = ByteBuffer.wrap(node.getFrame().getBody());

        // Signature, should always be 0xFF
        if ((buffer.get() & 0xFF) != 0xFF) {
            throw new IllegalArgumentException("Not a Net/ROM routing packet");
        }

        sendingNode = getCallsign(buffer, 6, false);

        LOG.debug("Buffer size: + " + buffer.remaining() + " bytes");
        while (buffer.hasRemaining()) {
            try {
                String destinationNodeCallsign = getCallsign(buffer, 7, true);
                String destinationNodeMnemonic = getCallsign(buffer, 6, false);
                String neighbourNodeCallsign = getCallsign(buffer, 7, true);
                int bestQualityValue = buffer.get() & 0xFF;
                LOG.debug("Adding routing entry: " + destinationNodeCallsign+"/"+destinationNodeMnemonic+" via "+neighbourNodeCallsign+" with quality "+bestQualityValue);
                NetROMNode routedNode = new NetROMNode(node.getInterface(), destinationNodeCallsign, destinationNodeMnemonic, neighbourNodeCallsign, bestQualityValue);
                nodesInThisPacket.add(routedNode);
            } catch(Throwable e) {
                LOG.error(e.getMessage(),e);
            }
        }
    }

    /**
     * Get a sequence of 6 or 7 bytes representing a callsign with optional bit shifting
     */
    public String getCallsign(ByteBuffer buffer, int length, boolean shift) {
        byte[] callsign = new byte[length];
        for (int i = 0; i < length; i++) {
            if (shift) {
                callsign[i] = (byte) ((buffer.get() & 0xFF) >> 1);
            } else {
                callsign[i] = buffer.get();
            }
        }

        return new String(callsign).trim();
    }

    /**
     * Get the nodes in this packet
     * @return
     */
    public List<NetROMNode> getNodesInThisPacket() {
        return nodesInThisPacket;
    }


    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Net/ROM routing packet from ");
        builder.append(sendingNode);
        builder.append(":\r\n");
        for (NetROMNode node : nodesInThisPacket) {
            builder.append(node.toString());
            builder.append("\r\n");
        }
        return builder.toString();
    }



}
