package org.prowl.kisset.io;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.InterfaceDriver;
import org.prowl.kisset.ax25.*;
import org.prowl.kisset.comms.Service;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.util.Tools;

import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Implements a KISS type passthrough on a TCP connection
 * <p>
 * All drivers should implement the @InterfaceDriver annotation so that they can be discovered by the system.
 */
@InterfaceDriver(name = "KISS via TCP", description = "KISS over TCP/IP", uiName = "fx/TCPConnectionPreference.fxml")
public class KISSviaTCP extends Interface {

    private static final Log LOG = LogFactory.getLog("KISSviaTCP");

    private final String address;
    private final int port;
    private final String defaultOutgoingCallsign;

    private final int pacLen;
    private final int maxFrames;
    private final int baudRate;
    private final int frequency;
    private final int retries;
    private Socket socketConnection;

    private BasicTransmittingConnector anInterface;


    public KISSviaTCP(HierarchicalConfiguration config) {
        super(config);

        // The address and port of the KISS interface we intend to connect to (KISS over IP or Direwolf, etc)
        address = config.getString("ipAddress");
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

    }

    @Override
    public void start() throws IOException {
        running = true;

        // Check the slot is obtainable.
        if (port < 1) {
            throw new IOException("Configuration problem - port " + port + " needs to be greater than 0");
        }

        Tools.runOnThread(() -> {
            setup();
        });
    }


    public void setup() {

        InputStream in = null;
        OutputStream out = null;
        // Always try to connect until we are reconfigured or stopped.
        while (running) {
            Tools.delay(1000);
            try {
                LOG.info("Connecting to kiss service at: " + address + ":" + port);
                socketConnection = new Socket(InetAddress.getByName(address), port);
                in = new BufferedInputStream(socketConnection.getInputStream());
                out = new BufferedOutputStream(socketConnection.getOutputStream());
                interfaceStatus = new InterfaceStatus(InterfaceStatus.State.OK,null);
                LOG.info("Connected to kiss service at: " + address + ":" + port);
                break;
            } catch (ConnectException e) {
                interfaceStatus = new InterfaceStatus(InterfaceStatus.State.WARN, "Waiting 30s to connect due to: " + e.getMessage());
                LOG.warn("Delaying 30s due to unable to connect to " + address + ":" + port + ": " + e.getMessage());
                Tools.delay(30000);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);

            }
        }

        if (in == null || out == null) {
            interfaceStatus = new InterfaceStatus(InterfaceStatus.State.ERROR, "Could not connect to remote KISS service at: " + address + ":" + port);
            LOG.error("Unable to connect to kiss service at: " + address + ":" + port + " - this connector is stopping.");
            running = false;
            return;
        }

        // Our default callsign. acceptInbound can determine if we actually want to accept any callsign requests,
        // not just this one.
        AX25Callsign defaultCallsign = new AX25Callsign(defaultOutgoingCallsign);

        anInterface = new BasicTransmittingConnector(getUUID(), pacLen, maxFrames, baudRate, retries, defaultCallsign, in, out, new ConnectionRequestListener() {
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
            public boolean acceptInbound(ConnState state, AX25Callsign originator, Connector port) {
                return checkInboundConnection(state, originator, port);
            }


            @Override
            public boolean isLocal(String callsign) {
                for (Service service : services) {
                    if (service.getCallsign().equalsIgnoreCase(callsign)) {
                        return true;
                    }
                }
                return false;
            }

        });

        // Tag for debug logs so we know what instance/frequency this connector is
        //  connector.setDebugTag(Tools.getNiceFrequency(frequency));

        // AX Frame listener for things like mheard lists
        anInterface.addFrameListener(new AX25FrameListener() {
            @Override
            public void consumeAX25Frame(AX25Frame frame, Connector connector) {
                LOG.debug("Got frame: " + frame.toString() + "  body=" + Tools.byteArrayToHexString(frame.getBody()));

                Node node = new Node(KISSviaTCP.this, frame.sender.toString(), frame.rcptTime, frame.dest.toString(), frame);

                // Determine the nodes capabilities from the frame type and add this to the node
                Tools.determineCapabilities(node, frame);

                // Fire off to anything that wants to know about nodes heard
                SingleThreadBus.INSTANCE.post(new HeardNodeEvent(node));
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
    public void setupConnectionListener(ConnState state, AX25Callsign originator, Connector port) {
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
        running = false;
        if (socketConnection != null) {
            try {
                socketConnection.close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + address + ":" + port + ")";
    }

    @Override
    public boolean connect(String to, String from, ConnectionEstablishmentListener connectionEstablishmentListener) throws IOException {

        if (anInterface == null) {
            throw new IOException("TCP/IP interface to '" + address + ":" + port + "' did not complete startup - please check configuration");
        }

        anInterface.makeConnection(from, to, connectionEstablishmentListener);

        return true;
    }

    @Override
    public void cancelConnection(Stream stream) {
        anInterface.cancelConnection(KISSet.INSTANCE.getMyCall(), stream.getRemoteCall());
    }

    @Override
    public void disconnect(Stream currentStream) {
        anInterface.disconnect(KISSet.INSTANCE.getMyCall(), currentStream.getRemoteCall());
    }

}
