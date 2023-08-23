package org.prowl.kisset.protocols.aprs;

import net.ab0oo.aprs.parser.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.APRSPacketEvent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
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

    public static final String SERVER = "rotate.aprs.net";
    public static final int PORT = 14580; // 10152 full feed, 14580 filtered feed

    private Config config;
    private Timer timer;

    private boolean running = false;

    // testing
    double lat = 52.0542919;
    double lon = -0.7594734;


    private APRSISClient() {
        config = KISSet.INSTANCE.getConfig();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                connect();
            }
        }, 10000, 60000);
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


        try {
            Socket s = new Socket(InetAddress.getByName(SERVER), PORT);
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

            out.write(("user APRSPR-TS pass -1 filter r/" + lat + "/" + lon + "/100\n").getBytes());
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in), 32768);

            while (!s.isClosed() && running) {

                String line = reader.readLine();
                if (!line.startsWith("#")) {
                    parsePacket(line, System.currentTimeMillis());
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void parsePacket(String packet, long time) {

        try {
            APRSPacket p = Parser.parse(packet);
            InformationField f = p.getAprsInformation();
            PositionField positionField = (PositionField) f.getAprsData(APRSTypes.T_POSITION);
            if (positionField != null) {
                Position pos = positionField.getPosition();

                SingleThreadBus.INSTANCE.post(new APRSPacketEvent(p));


                //     APRSLookup.INSTANCE.add(p, pos);
            } else {
                //  System.err.println("No position for:" +packet);
            }
        } catch (UnparsablePositionException e) {
            // Don't care
        } catch (Throwable e) {
            System.err.println(packet);
            e.printStackTrace();

        }
    }


}