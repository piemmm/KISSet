package org.prowl.kisset.io;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.ax25.*;
import org.prowl.kisset.comms.Service;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.objects.user.User;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public abstract class Interface {

    private static final Log LOG = LogFactory.getLog("Interface");


    protected BasicTransmittingConnector anInterface;

    // If the interface fails to start, the reason will be logged here.
    String failReason;
    private int currentStream = 0;
    private List<Stream> streams = new ArrayList<>();
    protected HierarchicalConfiguration config;
    private String uuid;
    private Timer beaconTimer;
    protected boolean running = true;

    public Interface(HierarchicalConfiguration config) {
        this.config = config;
        getUUID();

        // Create default streams.
        for (int i = 0; i < 7; i++) {
            streams.add(new Stream(this));
        }

        // Setup the beaconing
        setBeacon(config.getString(Conf.beaconText.name(), Conf.beaconText.stringDefault()), config.getInt(Conf.beaconEvery.name(), Conf.beaconEvery.intDefault()));
    }

    public abstract void start() throws IOException;

    public abstract void stop();

    public abstract boolean connect(String to, String from, ConnectionEstablishmentListener connectionEstablishmentListener) throws IOException;

    public abstract void disconnect(Stream currentStream);

    public abstract void cancelConnection(Stream stream);

    public Stream getStream(int stream) {
        return streams.get(stream);
    }

    public Stream getCurrentStream() {
        return streams.get(currentStream);
    }

    public void setCurrentStream(int currentStream) {
        this.currentStream = currentStream;
    }

    public List<Stream> getStreams() {
        return new ArrayList<>(streams);
    }

    public String getFailReason() {
        return failReason;
    }


    public String getUUID() {
        uuid = config.getString(Conf.uuid.name());
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
        config.setProperty(Conf.uuid.name(), uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interface that = (Interface) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }


    /**
     * Periodically send a beacon UI frame
     *
     * @param text
     * @param intervalMinutes
     */
    public void setBeacon(String text, int intervalMinutes) {
        if (beaconTimer != null) {
            beaconTimer.cancel();
        }

        // If interval is zero or negative then don't send beacons
        if (intervalMinutes <= 0) {
            return;
        }

        beaconTimer = new Timer();
        TimerTask beaconTimerTask = new TimerTask() {
            @Override
            public void run() {

                if (anInterface == null || !running) {
                    beaconTimer.cancel();
                    return;
                }

                anInterface.sendUI("BEACON", text.getBytes());
            }
        };
        beaconTimer.schedule(beaconTimerTask, 5000, intervalMinutes * 60 * 1000);
    }

    public boolean checkInboundConnection(ConnState state, AX25Callsign originator, Connector port) {
        // Check PMS
        for (Service service : KISSet.INSTANCE.getServices()) {
            if (service.getCallsign() != null && state.getDst().toString().equalsIgnoreCase(service.getCallsign())) {
                setupConnectionListener(service, state, originator, port);
                return true;
            }
        }
        return false;
    }

    /**
     * A connection has been accepted therefore we will set it up and also a listener to handle state changes
     *
     * @param state
     * @param originator
     * @param port
     */
    private void setupConnectionListener(Service service, ConnState state, AX25Callsign originator, Connector port) {
        // If we're going to accept then add a listener so we can keep track of this connection state
        state.listener = new ConnectionEstablishmentListener() {
            @Override
            public void connectionEstablished(Object sessionIdentifier, ConnState conn) {

                Thread tx = new Thread(() -> {
                    // Do inputty and outputty stream stuff here
                    try {
                        User user = new User();//KISSet.INSTANCE.getStorage().loadUser(conn.getSrc().getBaseCallsign());
                        user.setBaseCallsign(conn.getSrc().getBaseCallsign());
                        InputStream in = state.getInputStream();
                        OutputStream out = state.getOutputStream();

                        // This wrapper provides a simple way to terminate the connection when the outputstream
                        // is also closed.
                        OutputStream wrapped = new BufferedOutputStream(out) {
                            @Override
                            public void close() throws IOException {
                                conn.close();
                            }

                        };

                        service.acceptedConnection(user, in, wrapped);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                tx.start();

            }

            @Override
            public void connectionNotEstablished(Object sessionIdentifier, Object reason) {
                LOG.info("Connection not established from " + originator + " to " + state.getDst() + " for service " + service.getName());
            }

            @Override
            public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {
                LOG.info("Connection closed from " + originator + " to " + state.getDst() + " for service " + service.getName());
            }

            @Override
            public void connectionLost(Object sessionIdentifier, Object reason) {
                LOG.info("Connection lost from " + originator + " to " + state.getDst() + " for service " + service.getName());
            }
        };
    }

}
