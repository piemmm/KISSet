package org.prowl.kisset.io;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.InterfaceDriver;
import org.prowl.kisset.ax25.*;
import org.prowl.kisset.comms.Service;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Implements a KISS type connection with a serial port device
 */
@InterfaceDriver(name = "KISS via Serial", description = "KISS over Serial Port", uiName = "fx/SerialPortConnectionPreference.fxml")
public class KISSviaSerial extends Interface {

    private static final Log LOG = LogFactory.getLog("KISSviaSerial");
    private final String port;
    private final int dataBits;
    private final int stopBits;
    private final String parity;
    private final int serialBaudRate;
    private final String defaultOutgoingCallsign;
    private final int pacLen;
    private final int baudRate;
    private final int maxFrames;
    private final int frequency;
    private final int retries;
    private SerialPort serialPort = null; // The chosen port form our enumerated list.

    public KISSviaSerial(HierarchicalConfiguration config) {
        super(config);

        // The address and port of the KISS interface we intend to connect to (KISS over Serial)
        port = config.getString("serialPort");
        serialBaudRate = config.getInt("baudRate", 19200);
        dataBits = config.getInt("dataBits", 8);
        stopBits = config.getInt("stopBits", 1);
        parity = config.getString("parity", "N");

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


    public static List<SerialPort> getListOfSerialPorts() {
        // Rather than just use the port descriptor, we'll iterate through all the ports so we can at least see
        // what the system has available, so the user is not completely in the dark whe looking at logs.
        SerialPort[] ports = SerialPort.getCommPorts();
        return Arrays.asList(ports);
    }

    @Override
    public void start() throws IOException {


        // Check the slot is obtainable.
        if (port.length() < 1) {
            throw new IOException("Configuration problem - port " + port + " needs to be set correctly");
        }

        // Rather than just use the port descriptor, we'll iterate through all the ports so we can at least see
        // what the system has available, so the user is not completely in the dark whe looking at logs.
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort testPort : ports) {
            LOG.debug("Found serial port: " + testPort.getSystemPortName() + " - " + testPort.getSystemPortPath() + " - " + testPort.getDescriptivePortName()+ " - " + testPort.getPortDescription());
            if (testPort.getSystemPortPath().contains(port)) {
                serialPort = testPort;
            }
        }

        if (serialPort == null) {
            failReason = "Could not find serial port: " + port;
            LOG.warn(failReason);
            return;
        }
        LOG.debug(" ** Using serial port: " + serialPort.getSystemPortName());

        Tools.runOnThread(() -> {
            setup();
        });

    }

    public void setup() {

        int parityInt = SerialPort.NO_PARITY;
        if (parity.equalsIgnoreCase("E")) {
            parityInt = SerialPort.EVEN_PARITY;
        } else if (parity.equalsIgnoreCase("O")) {
            parityInt = SerialPort.ODD_PARITY;
        }


        serialPort.setBaudRate(serialBaudRate);
        serialPort.setParity(parityInt);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.openPort();
        //serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED | SerialPort.FLOW_CONTROL_DSR_ENABLED | SerialPort.FLOW_CONTROL_DTR_ENABLED);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        InputStream in = serialPort.getInputStream();
        OutputStream out = serialPort.getOutputStream();


        if (in == null || out == null) {
            LOG.error("Unable to connect to KISS device at: " + port + " - this connector is stopping.");
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
                for (Service service: services) {
                    if (service.getCallsign().equalsIgnoreCase(callsign)) {
                        return true;
                    }
                }
                return false;
            }
        });

        // Tag for debug logs so we know what instance/frequency this connector is
        //connector.setDebugTag(Tools.getNiceFrequency(frequency));

        // AX Frame listener for things like mheard lists
        anInterface.addFrameListener(new AX25FrameListener() {
            @Override
            public void consumeAX25Frame(AX25Frame frame, Connector connector) {
                LOG.debug("Got frame: " + frame.toString() + "  body=" + Tools.byteArrayToHexString(frame.getBody()));

                Node node = new Node(KISSviaSerial.this, frame.sender.toString(), frame.rcptTime, frame.dest.toString(), frame);

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
                LOG.info("Connection established from " + originator + " to " + state.getDst());

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
    public boolean connect(String to, String from, ConnectionEstablishmentListener connectionEstablishmentListener) throws IOException {

        // Interface will be null if the interface was not setup (not connected) or if the port was not found.
        if (anInterface == null) {
            throw new IOException("Serial Port interface on '"+ port +"' did not complete startup - please check configuration");
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

    @Override
    public void stop() {
        SingleThreadBus.INSTANCE.unregister(this);
        anInterface.stop();
        running = false;
        serialPort.closePort();
    }

    @Override
    public String toString() {
        if (serialPort == null) {
            return getClass().getSimpleName() + " ("+port+")";
        }
        return getClass().getSimpleName() + " (" + serialPort.toString() + "/" + serialPort.getSystemPortName() + ")";
    }

}
