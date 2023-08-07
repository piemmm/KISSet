package org.prowl.kisset.netrom;

import com.google.common.eventbus.Subscribe;
import org.prowl.kisset.ax25.AX25Frame;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;

/**
 * Listen to node packets and use it to build a list of routes
 */
public class RoutingListener {

    public RoutingListener() {
        SingleThreadBus.INSTANCE.register(this);
    }

    @Subscribe
    public void onHeardNode(HeardNodeEvent event) {
        if (event.getNode().getFrame().getPid() == AX25Frame.PID_NETROM) {
            NetROMRoutingPacket netROMRoutingPacket = new NetROMRoutingPacket(event.getNode());
            RoutingTable.INSTANCE.addNodes(netROMRoutingPacket.getNodesInThisPacket());
        }
    }
}
