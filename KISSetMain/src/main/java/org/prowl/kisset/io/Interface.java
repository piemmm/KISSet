package org.prowl.kisset.io;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.*;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.objects.user.User;
import org.prowl.kisset.services.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public abstract class Interface {

    private static final Log LOG = LogFactory.getLog("Interface");


    protected BasicTransmittingConnector anInterface;
    protected HierarchicalConfiguration config;
    protected boolean running = true;
    protected List<Service> services = new ArrayList<>();
    // If the interface fails to start, the reason will be logged here.
    InterfaceStatus interfaceStatus = new InterfaceStatus(InterfaceStatus.State.UP, null);
    private int currentStream = 0;
    private final List<Stream> streams = new ArrayList<>();
    private String uuid;
    private Timer beaconTimer;

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

    public int getPacLen() {
       return anInterface.getPacLen();
    }

    public abstract void start() throws IOException;

    public abstract void stop();

    public boolean isRunning() {
        return running;
    }

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

    public InterfaceStatus getInterfaceStatus() {
        return interfaceStatus;
    }

    public void setServices(List<Service> services) {
        this.services = services;
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
        beaconTimer.schedule(beaconTimerTask, 5000, (long) intervalMinutes * 60 * 1000);
    }

    public boolean checkInboundConnection(ConnState state, AX25Callsign originator, Connector port) {

        LOG.debug("Checking inbound connection: " + state.getSrc() + " to " + state.getDst());
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
                        user.setSourceCallsign(conn.getSrc().getBaseCallsign()+"-"+conn.getSrc().getSSID());
                        user.setDestinationCallsign(conn.getDst().getBaseCallsign()+"-"+conn.getSrc().getSSID());
                        AX25InputStream in = conn.getInputStream();
                        AX25OutputStream out = conn.getOutputStream();

                        // FIXME: This is a method to get the PID set correctly for netrom frames, I have no other use-cases
                        //        for this so I'm not sure if it's a good idea or not.
                        out.setPID(service.getServicePid(user));

                        service.acceptedConnection(Interface.this, user, in, out);
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

    /**
     * Get a kiss parameter from the interface
     * @param type
     * @return
     */
    public KissParameter getKissParameter(KissParameterType type) {
        return anInterface.getKISSParameter(type);
    }

    /**
     * Set a kiss parameter on the interface
     * @param type
     * @param data
     */
    public void setKissParameter(KissParameterType type, int[] data) {
        anInterface.setKISSParameter(type, data);
    }

    public void setKissParameter(KissParameterType type, int data) {
        anInterface.setKISSParameter(type, new int[] { data  });
    }

    /**
     * Convenience to send a UI frame
     * @param destination
     * @param data
     */
    public void sendUI(String destination, byte[] data) throws IOException {
        anInterface.sendUI(destination, data);
    }

    public void sendFrame(AX25Frame frame) throws IOException {
        anInterface.sendFrame(frame);
    }
}
