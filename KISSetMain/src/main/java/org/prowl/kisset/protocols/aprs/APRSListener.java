package org.prowl.kisset.protocols.aprs;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.aprslib.parser.APRSPacket;
import org.prowl.aprslib.parser.Parser;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.APRSPacketEvent;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;

public enum APRSListener {

    INSTANCE;

    private static final Log LOG = LogFactory.getLog("APRSListener");

    private Config config;

    APRSListener() {
        config = KISSet.INSTANCE.getConfig();
        // Load recent APRS packets from disk in case of an app restart
        //KISSet.INSTANCE.getStorage().loadRecentAPRSPackets();

        // Start listening for new DX spots
        SingleThreadBus.INSTANCE.register(this);
    }


    @Subscribe
    public void onHeardNode(HeardNodeEvent event) {
        if (config.getConfig(Conf.aprsDecoingOverKISSEnabled, Conf.aprsDecoingOverKISSEnabled.boolDefault())) {
            AX25Frame frame = event.getNode().getFrame();
            boolean isAprs = false;
            try {
                APRSPacket packet = Parser.parseAX25(frame.getRawPacket());
                // APRS packet reserialize the body
                //KISSet.INSTANCE.getStorage().addRecentAPRSPacket(aprsPacket);
                SingleThreadBus.INSTANCE.post(new APRSPacketEvent(packet));
                // }
            } catch (Throwable e) {
                // Ignore - probably not aprs. or unable to parse MICe
            }
        }
    }

}
