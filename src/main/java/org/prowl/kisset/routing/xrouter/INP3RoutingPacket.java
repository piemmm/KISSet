package org.prowl.kisset.routing.xrouter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.objects.routing.INP3Route;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class INP3RoutingPacket {

    private static final Log LOG = LogFactory.getLog("INP3RoutingPacket");

    private List<INP3Route> routes = new ArrayList<>();

    private String originCallsign; // The callsign this packet originated from
    private String destinationCallsign; // The advertised node callsign.
    private int hops;
    private int tripTime;
    private List<INP3Route.INP3Option> options = new ArrayList<>();

    public INP3RoutingPacket(Node node) {
        try {
            originCallsign = node.getCallsign();
            LOG.debug("INP3: originCallsign="+originCallsign);
            ByteBuffer buffer = ByteBuffer.wrap(node.getFrame().getBody());
            buffer.get(); // 0xFF

            destinationCallsign = PacketTools.getData(buffer, 7, true);
            hops = buffer.get() & 0xFF;
            tripTime = buffer.getShort() & 0xFFFF;

            LOG.debug("INP3: dest="+destinationCallsign+", hops="+hops+", tripTime="+tripTime);
            while (buffer.remaining() > 2) {
                int length = buffer.get() & 0xFF;
                LOG.debug("INP3: option length="+length);
                if (length > buffer.remaining()) {
                    LOG.error("INP3: option length is greater than remaining buffer length of "+length+" > "+buffer.remaining());
                }
                INP3Route.INP3OptionType type = INP3Route.INP3OptionType.fromValue(buffer.get() & 0xFF);
                LOG.debug("INP3: option type="+type);
                byte[] data = new byte[length];
                buffer.get(data);
                LOG.debug("INP3: option data="+ Tools.byteArrayToReadableASCIIString(data));
                options.add(new INP3Route.INP3Option(type, data));
            }
            // end frame marker is 0x00
            int endMarker = buffer.get();
            if (endMarker == 0x00) {
                LOG.debug("INP3: end marker found, adding route.");
                routes.add(new INP3Route(node.getInterface(), originCallsign, destinationCallsign, hops, tripTime, options));
            } else {
                // Invalid INP3 frame not following current documentation for INP3 - log it.
                LOG.error("Invalid INP3 frame from " + originCallsign + " detected:" + Tools.byteArrayToReadableASCIIString(node.getFrame().getBody()));
            }

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
        for (INP3Route route: getRoutes()) {
            builder.append(route);
            builder.append("\r\n");
        }

        return builder.toString();
    }
}
