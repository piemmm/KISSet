package org.prowl.kisset.protocols.netrom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.objects.routing.NetROMRoute;
import org.prowl.kisset.protocols.core.Node;
import org.prowl.kisset.util.PacketTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes a Net/ROM routing broadcast packet from an AX.25 frame
 */
public class NetROMRoutingPacket {
    private static final Log LOG = LogFactory.getLog("NetROMRoutingPacket");

    private String sendingNode;

    private final List<NetROMRoute> nodesInThisPacket = new ArrayList<>();

    /**
     * Parse a netrom routing pocket. These appear to be loosely based on the ax.25 address spec,
     * though no idea if the HDLC bit is used.
     *
     * @param node
     */
    public NetROMRoutingPacket(Node node) throws ParseException {
        byte[] body = node.getFrame().getBody();
        ByteBuffer buffer = ByteBuffer.wrap(body);

        // Signature, first byte should always be 0xFF, and last not 0x00
        int lastByte = body[body.length - 1] & 0xFF;
        int firstByte = buffer.get() & 0xFF;
        if (firstByte == 0xFF && lastByte != 0x00) {
            sendingNode = PacketTools.getData(buffer, 6, false);
            LOG.debug("Buffer size: + " + buffer.remaining() + " bytes");
            while (buffer.hasRemaining()) {
                try {
                    String destinationNodeCallsign = PacketTools.getData(buffer, 7, true);
                    String destinationNodeMnemonic = PacketTools.getData(buffer, 6, false);
                    String neighbourNodeCallsign = PacketTools.getData(buffer, 7, true);
                    int bestQualityValue = buffer.get() & 0xFF;
                    LOG.debug("Adding routing entry: " + destinationNodeCallsign + "/" + destinationNodeMnemonic + " via " + neighbourNodeCallsign + " with quality " + bestQualityValue);
                    NetROMRoute routedNode = new NetROMRoute(node.getInterface(), node.getCallsign(), destinationNodeCallsign, destinationNodeMnemonic, neighbourNodeCallsign, bestQualityValue);
                    nodesInThisPacket.add(routedNode);
                } catch (Throwable e) {
                    // If the packet was corrupt, then don't trust anything in it.
                    nodesInThisPacket.clear();
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Create a new Net/ROM routing packet
     */
    public NetROMRoutingPacket() {

    }

    /**
     * Add a node to the packet
     */
    public void addNode(NetROMRoute node) {
        nodesInThisPacket.add(node);
    }

    /**
     * Get the nodes in this packet
     *
     * @return
     */
    public List<NetROMRoute> getRoutesInThisPacket() {
        return nodesInThisPacket;
    }


    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Net/ROM routing packet from ");
        builder.append(sendingNode);
        builder.append(":\r\n");
        for (NetROMRoute node : nodesInThisPacket) {
            builder.append(node.toString());
            builder.append("\r\n");
        }
        return builder.toString();
    }

    /**
     * From the data in this class, create the raw packet.
     *
     * @return
     */
    public byte[] toPacketBody(String sendingCallsign) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            buffer.write((byte) 0xFF);
            byte[] encoded = new AX25Callsign(sendingCallsign).toByteArray(6,false);
            buffer.write(encoded);
            for (NetROMRoute node : nodesInThisPacket) {
                encoded = PacketTools.shiftLeft(new AX25Callsign(node.getDestinationNodeCallsign()).toByteArray(7,false));
                buffer.write(encoded);
                encoded = new AX25Callsign(node.getDestinationNodeMnemonic()).toByteArray(6,false);
                buffer.write(encoded);
                encoded = PacketTools.shiftLeft(new AX25Callsign(node.getNeighbourNodeCallsign()).toByteArray(7,false));
                buffer.write(encoded);
                buffer.write((byte) node.getBestQualityValue());
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return buffer.toByteArray();
    }

}
