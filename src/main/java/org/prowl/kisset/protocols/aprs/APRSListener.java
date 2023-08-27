package org.prowl.kisset.protocols.aprs;

import com.google.common.eventbus.Subscribe;
import net.ab0oo.aprs.parser.APRSPacket;
import net.ab0oo.aprs.parser.Parser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.APRSPacketEvent;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;

public enum APRSListener {

    INSTANCE;

    private static final Log LOG = LogFactory.getLog("APRSListener");

    APRSListener() {
        // Load recent APRS packets from disk in case of an app restart
        //KISSet.INSTANCE.getStorage().loadRecentAPRSPackets();

        // Start listening for new DX spots
        SingleThreadBus.INSTANCE.register(this);
    }


    @Subscribe
    public void onHeardNode(HeardNodeEvent event) {

        AX25Frame frame = event.getNode().getFrame();
        boolean isAprs = false;
        try {
            String aprsString = frame.sender.toString() + ">" + frame.dest.toString() + ":" + frame.getAsciiFrame();
            APRSPacket packet = Parser.parse(aprsString);
           // if (packet.getType() != APRSTypes.UNSPECIFIED) {
                // APRS packet reserialize the body
                //KISSet.INSTANCE.getStorage().addRecentAPRSPacket(aprsPacket);
                SingleThreadBus.INSTANCE.post(new APRSPacketEvent(packet));
           // }
        } catch (Throwable e) {
            // Ignore - probably not aprs. or unable to parse MICe
        }


    }

}
