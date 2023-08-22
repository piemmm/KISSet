package org.prowl.kisset.protocols.xrouter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.objects.routing.INP3Route;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Decodes an INP3 routing packet.
 */
public class INP3RoutingPacket {

    private static final Log LOG = LogFactory.getLog("INP3RoutingPacket");

    private List<INP3Route> routes = new ArrayList<>();

    private String originCallsign; // The callsign this packet originated from
    private String destinationCallsign; // The advertised node callsign.
    private int hops;
    private int tripTime;

    public INP3RoutingPacket(Node node) {
        try {
            originCallsign = node.getCallsign();
            LOG.debug("originCallsign=" + originCallsign);
            ByteBuffer buffer = ByteBuffer.wrap(node.getFrame().getBody());
            buffer.get(); // 0xFF

            // RIF header
            destinationCallsign = PacketTools.getData(buffer, 7, true);

            hops = buffer.get() & 0xFF;
            tripTime = buffer.getShort() & 0xFFFF;
            List<INP3Route.INP3Option> options = new ArrayList<>();
            INP3Route inp3Route = new INP3Route(node.getInterface(), originCallsign, destinationCallsign, hops, tripTime, options);
            LOG.debug("dest=" + destinationCallsign + ", hops=" + hops + ", tripTime=" + tripTime);

            do {

                // Standard RIP inside RIF
                int length = (buffer.get() & 0xFF)-2;

                // Check the packet hasn't ended.
                if (length < 0) {
                    // End of packet marker.  We follow spec so still exit.
                    if (buffer.remaining() > 0) {
                        // Screwed packet
                        LOG.warn("packet ended but there is still data in the buffer:" + buffer.remaining() + " bytes");
                    }
                    routes.add(inp3Route);
                    return; // End of packet
                }
                LOG.debug("option length=" + length);

                // Sense check on packet length
                if (length > buffer.remaining()) {
                    LOG.error("option length is greater than remaining buffer length: " + length + " > " + buffer.remaining());
                    return; // Invalid packet
                }
                int typeId = buffer.get() & 0xFF;
                INP3Route.INP3OptionType type = INP3Route.INP3OptionType.fromValue(typeId);
                LOG.debug("option type=" + type + "(0x" + Integer.toString(typeId, 16) + ")");

                byte[] data = new byte[length];
                buffer.get(data);
                LOG.debug("option data=" + Tools.byteArrayToReadableASCIIString(data));
                options.add(new INP3Route.INP3Option(type, data));

            } while (buffer.remaining() > 0);

        } catch (Throwable e) {
            LOG.error("Invalid INP3 frame from " + originCallsign + " detected:" + Tools.byteArrayToReadableASCIIString(node.getFrame().getBody()));
        }
    }

    /**
     * @return the decoded route, or null if the packet was corrupt.
     */
    public List<INP3Route> getRoutes() {
        return routes;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("INP3 routing packet from ");
        builder.append(originCallsign);
        builder.append(":\r\n");
        for (INP3Route route : getRoutes()) {
            builder.append(route);
            builder.append("\r\n");
        }

        return builder.toString();
    }
}
