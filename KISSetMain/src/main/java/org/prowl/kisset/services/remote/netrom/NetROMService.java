package org.prowl.kisset.services.remote.netrom;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.AX25Callsign;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.routing.NetROMRoute;
import org.prowl.kisset.objects.user.User;
import org.prowl.kisset.protocols.netrom.NetROMRoutingPacket;
import org.prowl.kisset.services.Service;
import org.prowl.kisset.services.remote.pms.PMSClientHandler;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The NetROM Service in KISSet is a basic implementation with the aim of understanding it's
 * surrounding topology and participating as a node in the network so that other nodes can hop through
 * it to reach their destination.
 */
public class NetROMService extends Service {

    private static final Log LOG = LogFactory.getLog("PMSService");

    private boolean stop = false;
    private final String callsign;
    private  String alias;

    /**
     * This is a map of all the clients that are connected to us.
     */
    private final Map<User, NetROMClientHandler> clients = Collections.synchronizedMap(new HashMap<>());

    public NetROMService(String name, String callsign) {
        super(name);
        LOG.debug("Starting Net/ROM Service, listening as " + callsign);
        this.callsign = callsign;

        // Get the alias
        Config config = KISSet.INSTANCE.getConfig();
        alias = config.getConfig(Conf.netromAlias, Conf.createDefaultNetromAlias());


        // We want to listen for packets.
        SingleThreadBus.INSTANCE.register(this);

        Tools.runOnThread(new Runnable() {
            @Override
            public void run() {
                Tools.delay(5000);
                sendNodeBroadcast();
            }
        });
    }

    public void acceptedConnection(Interface anInterface, User user, InputStream in, OutputStream out) {
        NetROMClientHandler client = new NetROMClientHandler(this, anInterface, user, in, out);
        client.start();
    }

    public void clientDisconnected(Interface anInterface, User user) {
        clients.remove(user);
    }

    public void start() {
    }

    public void stop() {
        stop = true;
    }

    @Override
    public String getCallsign() {
        return callsign;
    }

    /**
     * Return the node alias
     * @return
     */
    public String getAlias() {
        return alias;
    }

    @Subscribe
    public void handlePacket(HeardNodeEvent packet) {
       // LOG.debug("Received packet: " + packet);
//        if (packet.getNode().getDestination().equalsIgnoreCase(callsign)) {
//            // Packet sent to us, so we're interested in it.
//
//            // Some form of keepalive/quality tracking packet from BPQ:
//            // GB7MNK-1>M0NCW[S RR P NR=5 CTL=0xb1 PID=0xf0]{No LVL3}
//            // We can ignore this as our stack /should/ send a reply automatically.
//        }
    }

    /**
     * Send a netrom node broadcast for our node only (just to announce ourselves)
     */
    public void sendNodeBroadcast() {

        // Get all interfaces
        List<Interface> interfaceList = KISSet.INSTANCE.getInterfaceHandler().getInterfaces();

        // For each interface send our announcement
        for (Interface anInterface : interfaceList) {

            try {
                // Create our packet.
                NetROMRoutingPacket packet = new NetROMRoutingPacket();
                NetROMRoute route = new NetROMRoute(anInterface, callsign, callsign, getAlias(), callsign, 255);
                packet.addNode(route);

                byte[] anounceBody = packet.toPacketBody(alias);

                AX25Frame uiFrame = new AX25Frame(0xCF);
                uiFrame.sender = new AX25Callsign(callsign);
                uiFrame.dest = new AX25Callsign("NODES");
                uiFrame.setCmd(true);
                uiFrame.ctl = (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_UI);
                uiFrame.body = anounceBody;
                anInterface.sendFrame(uiFrame);

            } catch(IOException e) {
                LOG.error(e.getMessage(), e);
            }

        }

    }

}
