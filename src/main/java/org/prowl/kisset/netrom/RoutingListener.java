package org.prowl.kisset.netrom;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.ax25.AX25Frame;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;

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
                NetROMRoutingPacket netROMRoutingPacket = new NetROMRoutingPacket(event.getNode());
                RoutingTable.INSTANCE.addNodes(netROMRoutingPacket.getNodesInThisPacket());
                KISSet.INSTANCE.getStorage().saveNetROMRoutingTable();
            }
        } catch(Throwable e) {
            LOG.error(e.getMessage(),e);
        }
    }
}
