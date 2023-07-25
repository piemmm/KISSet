//package org.prowl.kisset.io.bluetooth;
//
//import org.apache.commons.configuration.HierarchicalConfiguration;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.prowl.kissterm.KISSterm;
//import org.prowl.kissterm.ax25.*;
//import org.prowl.kissterm.ax25.io.BasicTransmittingConnector;
//import org.prowl.kissterm.io.Interface;
//import org.prowl.kissterm.util.Tools;
//
//import javax.bluetooth.*;
//import javax.microedition.io.StreamConnection;
//import java.io.BufferedOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
///**
// * Implements a KISS type passthrough on a bluetooth serial port (Kenwood TH-D74, etc)
// */
//public class KISSviaBluetooth extends Interface {
//
//
//    private static final Log LOG = LogFactory.getLog("KISSviaBluetooth");
//
//    private String deviceName;
//    private String deviceUrl;
//    private int dataBits;
//    private int stopBits;
//    private String parity;
//    private int serialBaudRate;
//    private String defaultOutgoingCallsign;
//
//
//    private int pacLen;
//    private int baudRate;
//    private int maxFrames;
//    private int frequency;
//    private int retries;
//
//    private BasicTransmittingConnector connector;
//    private HierarchicalConfiguration config;
//    private boolean running;
//    private RemoteDevice remoteDevice = null; // The chosen port form our enumerated list.
//    private boolean scanFinished = false;
//
//    public KISSviaBluetooth(HierarchicalConfiguration config) {
//        this.config = config;
//    }
//
//    @Override
//    public void start() throws IOException {
//        running = true;
//
//        // The address and port of the KISS interface we intend to connect to (KISS over Serial)
//        deviceName = config.getString("deviceName");
//
//        // This is the default callsign used for any frames sent out not using a registered service(with its own call).
//        // So if I were to say at node level 'broadcast this UI frame on all interfaces' it would use this callsign.
//        // But if a service wanted to do the same, (eg: BBS service sending an FBB list) then it would use the service
//        // callsign instead.
//        defaultOutgoingCallsign = KISSterm.INSTANCE.getMyCall();
//
//        // Settings for timeouts, max frames a
//        pacLen = config.getInt("pacLen", 120);
//        baudRate = config.getInt("channelBaudRate", 1200);
//        maxFrames = config.getInt("maxFrames", 3);
//        frequency = config.getInt("frequency", 0);
//        retries = config.getInt("retries", 6);
//
//        // Check the slot is obtainable.
//        if (deviceName.length() < 1) {
//            throw new IOException("Configuration problem - port " + deviceName + " needs to be set correctly");
//        }
//
//        Tools.runOnThread(() -> {
//            setup();
//        });
//    }
//
//    public void setup() {
//
//
//
//        try {
//            LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, new DiscoveryListener() {
//                @Override
//                public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
//                    try {
//                        String name = btDevice.getFriendlyName(false);
//                        System.out.format("%s (%s)\n", name, btDevice.getBluetoothAddress());
//                        if (name.matches("HC.*")) {
//                            remoteDevice = btDevice;
//                            System.out.println("got it!");
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void inquiryCompleted(int discType) {
//                    scanFinished = true;
//                }
//
//                @Override
//                public void serviceSearchCompleted(int transID, int respCode) {
//                }
//
//                @Override
//                public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
//                }
//            });
//
//
//        while (!scanFinished) {
//            //this is easier to understand (for me) as the thread stuff examples from bluecove
//            Tools.delay(500);
//        }
//
//        //search for services:
//        UUID uuid = new UUID(0x1101); //scan for btspp://... services (as HC-05 offers it)
//        UUID[] searchUuidSet = new UUID[]{uuid};
//        int[] attrIDs = new int[]{
//                0x0100 // service name
//        };
//        scanFinished = false;
//        LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs, searchUuidSet,
//                remoteDevice, new DiscoveryListener() {
//                    @Override
//                    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
//                    }
//
//                    @Override
//                    public void inquiryCompleted(int discType) {
//                    }
//
//                    @Override
//                    public void serviceSearchCompleted(int transID, int respCode) {
//                        scanFinished = true;
//                    }
//
//                    @Override
//                    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
//                        for (int i = 0; i < servRecord.length; i++) {
//                            deviceUrl = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
//                            if (deviceUrl != null) {
//                                break; //take the first one
//                            }
//                        }
//                    }
//                });
//
//        while (!scanFinished) {
//            Tools.delay(500);
//        }
//
//        System.out.println(remoteDevice.getBluetoothAddress());
//        System.out.println(deviceUrl);
//
//        //if you know your hc05Url this is all you need:
//        StreamConnection streamConnection = (StreamConnection) javax.microedition.io.Connector.open(deviceUrl);
//        OutputStream out = streamConnection.openOutputStream();
//        InputStream in = streamConnection.openInputStream();
//        //streamConnection.close();
//
//
//        if (in == null || out == null) {
//            LOG.error("Unable to connect to kiss service at: " + deviceName + " - this connector is stopping.");
//            return;
//        }
//        LOG.info("Connection made to bluetooth device: " + deviceName+"   "+deviceUrl);
//
//        // Our default callsign. acceptInbound can determine if we actually want to accept any callsign requests,
//        // not just this one.
//        AX25Callsign defaultCallsign = new AX25Callsign(defaultOutgoingCallsign);
//
//        connector = new BasicTransmittingConnector(pacLen, maxFrames, baudRate, retries, defaultCallsign, in, out, new ConnectionRequestListener() {
//            /**
//             * Determine if we want to respond to this connection request (to *ANY* callsign) - usually we only accept
//             * if we are interested in the callsign being sent a connection request.
//             *
//             * @param state      ConnState object describing the session being built
//             * @param originator AX25Callsign of the originating station
//             * @param port       Connector through which the request was received
//             * @return
//             */
//            @Override
//            public boolean acceptInbound(ConnState state, AX25Callsign originator, Connector port) {
//                LOG.info("Incoming connection request from " + originator + " to " + state.getDst() );
//
//                //setupConnectionListener(state, originator, port);
//
//
//                // Do not accept (possibly replace this with a default handler to display a message in the future?)
//                // Maybe use the remoteUISwitch to do it?
//                LOG.info("Rejecting connection request from " + originator + " to " + state.getDst() + " as no service is registered for this callsign");
//                return false;
//            }
//        });
//
//        // Tag for debug logs so we know what instance/frequency this connector is
//        //connector.setDebugTag(Tools.getNiceFrequency(frequency));
//
//        // AX Frame listener for things like mheard lists
//        connector.addFrameListener(new AX25FrameListener() {
//            @Override
//            public void consumeAX25Frame(AX25Frame frame, Connector connector) {
//                // Create a node to represent what we've seen - we'll merge this in things like
//                // mheard lists if there is another node there so that capability lists can grow
//                //Node node = new Node(KISSviaBluetooth.this, frame.sender.toString(), frame.rcptTime, frame.dest.toString(), frame);
//
//                // Determine the nodes capabilities from the frame type and add this to the node
//                //PacketTools.determineCapabilities(node, frame);
//
//                // Fire off to anything that wants to know about nodes heard
//                //ServerBus.INSTANCE.post(new HeardNodeEvent(node));
//            }
//        });
//
//        } catch (BluetoothStateException e) {
//            LOG.error(e.getMessage(),e);
//        } catch(IOException e) {
//            LOG.error(e.getMessage(),e);
//        }
//    }
//
//    /**
//     * A connection has been accepted therefore we will set it up and also a listener to handle state changes
//     *
//     * @param state
//     * @param originator
//     * @param port
//     */
//    public void setupConnectionListener(ConnState state, AX25Callsign originator, Connector port) {
//        // If we're going to accept then add a listener so we can keep track of this connection state
//        state.listener = new ConnectionEstablishmentListener() {
//            @Override
//            public void connectionEstablished(Object sessionIdentifier, ConnState conn) {
//                LOG.info("Connection established from " + originator + " to " + state.getDst() );
//
//            }
//
//            @Override
//            public void connectionNotEstablished(Object sessionIdentifier, Object reason) {
//                LOG.info("Connection not established from " + originator + " to " + state.getDst() );
//            }
//
//            @Override
//            public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {
//                LOG.info("Connection closed from " + originator + " to " + state.getDst());
//            }
//
//            @Override
//            public void connectionLost(Object sessionIdentifier, Object reason) {
//                LOG.info("Connection lost from " + originator + " to " + state.getDst());
//            }
//        };
//    }
//
//    @Override
//    public void stop() {
//        //ServerBus.INSTANCE.unregister(this);
//        running = false;
//    }
//
//    @Override
//    public String getName() {
//        return getClass().getSimpleName();
//    }
//
//
//
//}
