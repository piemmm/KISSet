package org.prowl.kisset.protocols.aprs;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.aprslib.parser.*;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.APRSPacketEvent;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Deque;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Responsible for keeping a connection to aprs-is
 *
 * @author ihawkins
 */
public enum APRSISClient {

    INSTANCE;

    private static final Log LOG = LogFactory.getLog("APRSISClient");

    public String server = "rotate.aprs.net";
    public int port = 14580; // 10152 full feed, 14580 filtered feed
    // testing
    double lat = 52.0542919;
    double lon = -0.7594734;
    private final Config config;
    private final Timer timer;
    private boolean running = false;
    private final int rangeKM = 200;


    APRSISClient() {
        config = KISSet.INSTANCE.getConfig();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                connect();
            }
        }, 4000, 60000);
        SingleThreadBus.INSTANCE.register(this);
    }

    public void stop() {
        timer.cancel();
        running = false;
    }

    public void connect() {
        running = true;
        boolean isEnabled = config.getConfig(Conf.connectToAPRSIServer, Conf.connectToAPRSIServer.boolDefault());
        if (!isEnabled) {
            return;
        }

        String serverPort = config.getConfig(Conf.aprsIServerHostname, Conf.aprsIServerHostname.stringDefault());

        // Get the server and port from server:port format (port may not be present)
        if (serverPort.contains(":")) {
            String[] parts = serverPort.split(":");
            server = parts[0];
            if (parts.length > 1) {
                try {
                    port = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    LOG.error("Invalid port number in APRS-IS server config: " + serverPort);
                }
            }
        } else {
            server = serverPort;
            port = 14580;
        }

        try {
            Socket s = new Socket(InetAddress.getByName(server), port);
            try {
                s.setSoTimeout(60000);
            } catch (Throwable e) {
            }
            try {
                s.setKeepAlive(true);
            } catch (Throwable e) {
            }
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();

            out.write(("user APRSPR-TS pass -1 filter r/" + lat + "/" + lon + "/" + rangeKM + "\n").getBytes());
           // out.write(("user APRSPR-TS pass -1\n\n").getBytes());

            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in), 32768);

            while (!s.isClosed() && running) {

                String line = reader.readLine();
                if (!line.startsWith("#")) {
                    parsePacket(line, System.currentTimeMillis());
                }
            }

            try {
                s.close();
            } catch (Throwable e) {
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    @Subscribe
    public void onConfigurationChanged(ConfigurationChangedEvent e) {
        running = false; // Force reconnection
    }


    public void parsePacket(String packet, long time) {

        try {
            APRSPacket p = Parser.parse(packet);
            InformationField f = p.getAprsInformation();
            PositionField positionField = (PositionField) f.getAprsData(APRSTypes.T_POSITION);
            if (positionField != null) {
                Position pos = positionField.getPosition();
                SingleThreadBus.INSTANCE.post(new APRSPacketEvent(p));
            } else {
               // LOG.debug("No position for:" +packet);
            }
        } catch (UnparsablePositionException e) {
            // Don't care
            LOG.debug("Unparseable position: " + packet);
            LOG.debug(e.getMessage(),e);
        } catch (Throwable e) {
            System.err.println(packet);
            e.printStackTrace();

        }
    }


}