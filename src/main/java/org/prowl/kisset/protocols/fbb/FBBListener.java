package org.prowl.kisset.protocols.fbb;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;

public enum FBBListener {

    INSTANCE;

    private static final Log LOG = LogFactory.getLog("FBBListener");

    FBBListener() {
        // Load FBB messages from disk
        //KISSet.INSTANCE.getStorage().loadFBBMessages();

        // Start listening for new FBB messages
        SingleThreadBus.INSTANCE.register(this);
    }

    @Subscribe
    public void onHeardNode(HeardNodeEvent event) {
        try {
           if (event.getNode().getFrame().getPid() != AX25Frame.PID_NOLVL3) {
               return;
           }

           if (!event.getNode().getDestination().equals("FBB")) {
               return;
           }

           byte[] body = event.getNode().getFrame().getBody();
           String data = new String(body);
           if (data.matches("^[0-9][0-9]+ ")) {
                // It's a FBB message
                //FBBMessage fbbMessage = new FBBMessage(event.getNode());
                //KISSet.INSTANCE.getStorage().addFBBMessage(fbbMessage);

           }



        } catch(Throwable e) {
            LOG.error(e.getMessage(),e);
        }
    }

}
