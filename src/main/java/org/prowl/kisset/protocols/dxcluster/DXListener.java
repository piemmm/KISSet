package org.prowl.kisset.protocols.dxcluster;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.DXSpotEvent;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.objects.dxcluster.DXSpot;

public enum DXListener {

    INSTANCE;

    private static final Log LOG = LogFactory.getLog("DXListener");

    DXListener() {
        // Load recent spots from disk in case of an app restart
        //KISSet.INSTANCE.getStorage().loadRecentDXClusterSpots();

        // Start listening for new DX spots
        SingleThreadBus.INSTANCE.register(this);
    }

    @Subscribe
    public void onHeardNode(HeardNodeEvent event) {
        try {
            if (event.getNode().getFrame().getPid() != AX25Frame.PID_NOLVL3) {
                return;
            }

            if (!event.getNode().getDestination().equals("DX")) {
                return;
            }

            byte[] body = event.getNode().getFrame().getBody();
            String data = new String(body);
            LOG.debug("DATA:" + data);
            if (data.startsWith("DX de")) {
                // It's a DX spot
                DXSpot dxSpot = new DXSpot(event.getNode());
                //KISSet.INSTANCE.getStorage().addRecentDXClusterSpot(dxSpot);
                SingleThreadBus.INSTANCE.post(new DXSpotEvent(dxSpot));
            }


        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
