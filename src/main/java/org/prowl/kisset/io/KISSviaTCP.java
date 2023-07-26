package org.prowl.kisset.io;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.InterfaceDriver;
import org.prowl.kisset.ax25.*;
import org.prowl.kisset.ax25.io.BasicTransmittingConnector;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.util.Tools;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;

/**
 * Implements a KISS type passthrough on a TCP connection
 *
 * All drivers should implement the @InterfaceDriver annotation so that they can be discovered by the system.
 */
@InterfaceDriver(name = "KISS via TCP", description = "KISS over TCP/IP", uiName="fx/TCPConnectionPreference.fxml")
public class KISSviaTCP extends Interface {

    private static final Log LOG = LogFactory.getLog("KISSviaTCP");

    private String address;
    private int port;
    private String defaultOutgoingCallsign;

    private int pacLen;
    private int maxFrames;
    private int baudRate;
    private int frequency;
    private int retries;

    private BasicTransmittingConnector connector;
    private HierarchicalConfiguration config;
    private boolean running;

    public KISSviaTCP(HierarchicalConfiguration config) {
        this.config = config;
    }

    @Override
    public void start() throws IOException {
        running = true;

        // The address and port of the KISS interface we intend to connect to (KISS over IP or Direwolf, etc)
        address = config.getString("address");
        port = config.getInt("port");

        // This is the default callsign used for any frames sent out not using a registered service(with its own call).
        // So if I were to say at node level 'broadcast this UI frame on all interfaces' it would use this callsign.
        // But if a service wanted to do the same, (eg: BBS service sending an FBB list) then it would use the service
        // callsign instead.
        defaultOutgoingCallsign = KISSet.INSTANCE.getMyCall();

        // Settings for timeouts, max frames a
        pacLen = config.getInt("pacLen", 120);
        baudRate = config.getInt("channelBaudRate", 1200);
        maxFrames = config.getInt("maxFrames", 3);
        frequency = config.getInt("frequency", 0);
        retries = config.getInt("retries", 6);

        // Check the slot is obtainable.
        if (port < 1) {
            throw new IOException("Configuration problem - port " + port + " needs to be greater than 0");
        }

        Tools.runOnThread(() -> {
            setup();
        });
    }

    @Override
    public String getUUID() {
        return config.getString("uuid");
    }

    public void setUUID(String uuid) {
        config.setProperty("uuid", uuid);
    }

    public void setup() {

        InputStream in = null;
        OutputStream out = null;
        int attempts = 10; // We try for a few attempts as this might be at boot and interfaces may still be coming up.
        while (attempts-- > 0 && running) {
            Tools.delay(1000);
            try {
                LOG.info("Connecting to kiss service at: " + address + ":" + port);
                Socket s = new Socket(InetAddress.getByName(address), port);
                in = s.getInputStream();
                out = s.getOutputStream();
                LOG.info("Connected to kiss service at: " + address + ":" + port);
                break;
            } catch (ConnectException e) {
                LOG.warn("Delaying 30s due to unable to connect to " + address + ":" + port + ": " + e.getMessage());
                Tools.delay(30000);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);

            }
        }

        if (in == null || out == null) {
            LOG.error("Unable to connect to kiss service at: " + address + ":" + port + " - this connector is stopping.");
            return;
        }

        // Our default callsign. acceptInbound can determine if we actually want to accept any callsign requests,
        // not just this one.
        AX25Callsign defaultCallsign = new AX25Callsign(defaultOutgoingCallsign);

        connector = new BasicTransmittingConnector(pacLen, maxFrames, baudRate, retries, defaultCallsign, in, out, new ConnectionRequestListener() {
            /**
             * Determine if we want to respond to this connection request (to *ANY* callsign) - usually we only accept
             * if we are interested in the callsign being sent a connection request.
             *
             * @param state      ConnState object describing the session being built
             * @param originator AX25Callsign of the originating station
             * @param port       Connector through which the request was received
             * @return
             */
            @Override
            public boolean acceptInbound(ConnState state, AX25Callsign originator, org.prowl.kisset.ax25.Connector port) {

                LOG.info("Incoming connection request from " + originator + " to " + state.getDst() );


                // Do not accept (possibly replace this with a default handler to display a message in the future?)
                // Maybe use the remoteUISwitch to do it?
                LOG.info("Rejecting connection request from " + originator + " to " + state.getDst() + " as no service is registered for this callsign");
                return false;
            }
        });

        // Tag for debug logs so we know what instance/frequency this connector is
      //  connector.setDebugTag(Tools.getNiceFrequency(frequency));

        // AX Frame listener for things like mheard lists
        connector.addFrameListener(new AX25FrameListener() {
            @Override
            public void consumeAX25Frame(AX25Frame frame, org.prowl.kisset.ax25.Connector connector) {
                // Create a node to represent what we've seen - we'll merge this in things like
                // mheard lists if there is another node there so that capability lists can grow
//                Node node = new Node(KISSviaTCP.this, frame.sender.toString(), frame.rcptTime, frame.dest.toString(), frame);
//
//                // Determine the nodes capabilities from the frame type and add this to the node
//                PacketTools.determineCapabilities(node, frame);
//
//                // Fire off to anything that wants to know about nodes heard
//                ServerBus.INSTANCE.post(new HeardNodeEvent(node));
            }
        });

    }


    /**
     * A connection has been accepted therefore we will set it up and also a listener to handle state changes
     *
     * @param state
     * @param originator
     * @param port
     */
    public void setupConnectionListener(ConnState state, AX25Callsign originator, org.prowl.kisset.ax25.Connector port) {
        // If we're going to accept then add a listener so we can keep track of this connection state
        state.listener = new ConnectionEstablishmentListener() {
            @Override
            public void connectionEstablished(Object sessionIdentifier, ConnState conn) {

                Thread tx = new Thread(() -> {
                    // Do inputty and outputty stream stuff here
                    try {
                        //User user = KISSterm.INSTANCE.getStorage().loadUser(conn.getSrc().getBaseCallsign());
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

                      //  service.acceptedConnection(user, in, wrapped);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                tx.start();

            }

            @Override
            public void connectionNotEstablished(Object sessionIdentifier, Object reason) {
                LOG.info("Connection not established from " + originator + " to " + state.getDst());
            }

            @Override
            public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {
                LOG.info("Connection closed from " + originator + " to " + state.getDst());
            }

            @Override
            public void connectionLost(Object sessionIdentifier, Object reason) {
                LOG.info("Connection lost from " + originator + " to " + state.getDst());
            }
        };
    }

    @Override
    public void stop() {
//        ServerBus.INSTANCE.unregister(this);
        running = false;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName()+" " + address + ":" + port;
    }


}
