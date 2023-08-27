package org.prowl.kisset.protocols;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.protocols.netrom.NetROMRoutingPacket;
import org.prowl.kisset.protocols.netrom.NetROMRoutingTable;
import org.prowl.kisset.protocols.xrouter.INP3RoutingPacket;
import org.prowl.kisset.protocols.xrouter.INP3RoutingTable;

/**
 * Listen to node packets and use it to build a list of routes
 */
public enum RoutingListener {

    INSTANCE;

    private static final Log LOG = LogFactory.getLog("RoutingListener");


    RoutingListener() {
        // Load the routing table from disk (expiring old entries)
        KISSet.INSTANCE.getStorage().loadNetROMRoutingTable();
        // Start listening for new routes
        SingleThreadBus.INSTANCE.register(this);
    }

    @Subscribe
    public void onHeardNode(HeardNodeEvent event) {
        try {
            if (event.getNode().getFrame().getPid() == AX25Frame.PID_NETROM) {
                byte[] body = event.getNode().getFrame().getBody();
                if ((body[0] & 0xFF) == 0xFF && ((body[body.length - 1] & 0xFF) != 0)) {
                    // It's a netrom packet
                    NetROMRoutingPacket netROMRoutingPacket = new NetROMRoutingPacket(event.getNode());
                    NetROMRoutingTable.INSTANCE.addRoutes(netROMRoutingPacket.getRoutesInThisPacket());
                    KISSet.INSTANCE.getStorage().saveNetROMRoutingTable();
                } else if ((body[0] & 0xFF) == 0xFF && ((body[body.length - 1] & 0xFF) == 0)) {
                    // It's an inp3 routing packet
                    INP3RoutingPacket inp3RoutingPacket = new INP3RoutingPacket(event.getNode());
                    INP3RoutingTable.INSTANCE.addRoutes(inp3RoutingPacket.getRoutes());
                    KISSet.INSTANCE.getStorage().saveNetROMRoutingTable();
                }

            }
        } catch(Throwable e) {
            LOG.error(e.getMessage(),e);
        }
    }
}
