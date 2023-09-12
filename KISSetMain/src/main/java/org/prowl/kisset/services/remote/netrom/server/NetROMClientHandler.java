package org.prowl.kisset.services.remote.netrom.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.routing.NetROMRoute;
import org.prowl.kisset.objects.user.User;
import org.prowl.kisset.protocols.netrom.NetROMPacket;
import org.prowl.kisset.protocols.netrom.NetROMRoutingTable;
import org.prowl.kisset.services.ClientHandler;
import org.prowl.kisset.services.Service;
import org.prowl.kisset.services.remote.netrom.circuit.Circuit;
import org.prowl.kisset.services.remote.netrom.circuit.CircuitException;
import org.prowl.kisset.services.remote.netrom.circuit.CircuitManager;
import org.prowl.kisset.services.remote.netrom.circuit.CircuitState;
import org.prowl.kisset.services.remote.netrom.opcodebeans.*;
import org.prowl.kisset.util.Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * This represents a Net/ROM connection to our node from another node.
 */
public class NetROMClientHandler implements ClientHandler {

    private static final Log LOG = LogFactory.getLog("NetROMClientHandler");

    private static final long TIMEOUT = 120000; // 2 minutes
    private static final long REASSEMBLY_TIMEOUT = 120000; // 2 minutes

    private InputStream in;
    private OutputStream out;
    private final User user;
    private final Interface anInterface;
    private BufferedReader bin;
    private NetROMServerService service;

    public NetROMClientHandler(NetROMServerService service, Interface anInterface, User user, InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.user = user;
        this.service = service;
        this.anInterface = anInterface;
    }

    public int getPacLen() {
        return anInterface.getPacLen();
    }

    /**
     * Read in packets from our connection and process them.
     */
    @Override
    public void start() {
        Tools.runOnThread(() -> {
            try {
                // Packet spec means they will always be <= 256 bytes.
                byte[] buffer = new byte[256];
                int b = 0;
                while (b != -1) {
                    // Handily as we process packet frames a chunk at a time, we can
                    // do the below to grab a 'netrom' packet, one at a time.
                    int lengthRead = in.read(buffer, 0, buffer.length);
                    byte[] data = new byte[lengthRead];
                    System.arraycopy(buffer, 0, data, 0, lengthRead);

                    if (lengthRead > 0) {
                        LOG.debug("Received " + lengthRead + " bytes: "+Tools.byteArrayToReadableASCIIString(data));
                        // We have a packet, let's process it.
                        NetROMPacket packet = new NetROMPacket(data);
                        processPacket(packet);
                    }
                }
            } catch (Throwable e) {
                LOG.error("Error in client handler", e);

            } finally {
                service.clientDisconnected(anInterface, user);
            }

            // TODO: Start another thread here to forward any data blocks to remote nodes if there is data waiting?
        });
    }


    /**
     * Process a netrom packet from a connected node according to the netrom spec.
     * <p>
     * A connected node could be conversing with us about several different circuits/things
     * so we will farm each of these off to their own individual handlers where needed.
     * <p>
     * Also the data could come from any node if a route decides to change.
     *
     * @param packet
     */
    public void processPacket(NetROMPacket packet) throws IOException {

        LOG.debug("Incoming packet:" + packet.toString());
        if (packet.getDestinationCallsign().toString().equalsIgnoreCase(service.getCallsign()) || packet.getDestinationCallsign().toString().equalsIgnoreCase(service.getAlias())) {
            // It's to us! we have to make circuits and stuff!
            LOG.debug("Sinking packet");
            sinkPacket(packet);
        } else {
            LOG.debug("Forwarding packet");
            // Just foward the packet to the next hop
            forwardPacket(packet);
        }
    }

    /**
     * The connection terminates or originates from us, so we will be managing it.
     *
     * @param packet
     * @throws IOException
     */
    public void sinkPacket(NetROMPacket packet) throws IOException {

        switch (packet.getOpCode()) {
            case NetROMPacket.OPCODE_CONNECT_REQUEST:
                // We have a connection request.
                LOG.debug("Got a connection request from " + packet.getOriginCallsign() + " to " + packet.getDestinationCallsign());
                receiveConnectionRequest(new ConnectRequest(packet));
                break;
            case NetROMPacket.OPCODE_CONNECT_ACK:
                // We have a connection ack.
                LOG.debug("Got a connection ack from " + packet.getOriginCallsign() + " to " + packet.getDestinationCallsign());
                receiveConnectionAcknowledge(new ConnectAcknowledge(packet));
                break;
            case NetROMPacket.OPCODE_DISCONNECT_REQUEST:
                // We have a disconnect request.
                LOG.debug("Got a disconnect request from " + packet.getOriginCallsign() + " to " + packet.getDestinationCallsign());
                DisconnectRequest disconnectRequest = new DisconnectRequest(packet);
                receiveDisconnectRequest(new DisconnectRequest(packet));
                break;
            case NetROMPacket.OPCODE_DISCONNECT_ACK:
                // We have a disconnect ack.
                LOG.debug("Got a disconnect ack from " + packet.getOriginCallsign() + " to " + packet.getDestinationCallsign());
                DisconnectAcknowledge disconnectAcknowledge = new DisconnectAcknowledge(packet);
                receiveDisconnectAck(new DisconnectAcknowledge(packet));
                break;
            case NetROMPacket.OPCODE_INFORMATION_TRANSFER:
                // We have a data packet.
                LOG.debug("Got a data packet from " + packet.getOriginCallsign() + " to " + packet.getDestinationCallsign());
                Information information = new Information(packet);
                receiveInformationTransfer(new Information(packet));
                break;
            case NetROMPacket.OPCODE_INFORMATION_ACK:
                // We have a data ack.
                LOG.debug("Got a data ack from " + packet.getOriginCallsign() + " to " + packet.getDestinationCallsign());
                InformationAcknowledge informationAcknowledge = new InformationAcknowledge(packet);
                receiveInformationAcknowledge(new InformationAcknowledge(packet));
                break;
            case NetROMPacket.OPCODE_RESET:
                // We have a reset.
                LOG.debug("Got a reset from " + packet.getOriginCallsign() + " to " + packet.getDestinationCallsign());
                Reset reset = new Reset(packet);

                break;
        }
    }


    /**
     * Deals with receiving a connection request.
     * <p>
     * We create the circuit, and send back a connection ack.
     *
     * @param connectRequest
     * @throws CircuitException
     */
    public void receiveConnectionRequest(ConnectRequest connectRequest) throws IOException {

        // Create a circuit for this connection.
        Circuit circuit = new Circuit();
        circuit.setSourceCallsign(connectRequest.getSourceCallsign());
        circuit.setDestinationCallsign(connectRequest.getDestinationCallsign());
        circuit.setOriginatingUser(connectRequest.getCallsignOfOriginatingUser());
        circuit.setOriginatingNode(connectRequest.getCallsignOfOriginatingNode());
        circuit.setAcceptedFrames(connectRequest.getProposeWindowSize());
        circuit.setYourCircuitIndex(connectRequest.getMyCircuitIndex());
        circuit.setYourCiruitID(connectRequest.getMyCircuitID());

        CircuitManager.registerCircuit(circuit, this); // Applies the circuit IDs and indexes.

        // Forward the connection to it's handler which is given the circuit input and circuit output streams.
        List<Service> services = KISSet.INSTANCE.getServices();
        Service chosen = null;
        for (Service service : services) {
            if (service.getCallsign().equals(circuit.getDestinationCallsign().toString()) && !(service instanceof NetROMServerService)) {
                chosen = service;
                break;
            }
        }

        // Send a connection ack/nack depending if the connection succeeded or failed
        ConnectAcknowledge connectAcknowledge = new ConnectAcknowledge();
        connectAcknowledge.setOriginCallsign(circuit.getDestinationCallsign());
        connectAcknowledge.setDestinationCallsign(circuit.getSourceCallsign());
        connectAcknowledge.setAcceptWindowSize(circuit.getAcceptedFrames());
        connectAcknowledge.setYourCircuitIndex(circuit.getYourCircuitIndex());
        connectAcknowledge.setYourCircuitID(circuit.getYourCircuitID());
        connectAcknowledge.setMyCircuitIndex(circuit.getMyCircuitIndex());
        connectAcknowledge.setMyCircuitID(circuit.getMyCircuitId());

        // If we refused the connection then set the high order bit (which is in ACK_REFUSED for convenience)
        connectAcknowledge.setOpcode(NetROMPacket.OPCODE_CONNECT_ACK);
        connectAcknowledge.setRefused(!circuit.isValid());

        // Send the packet.
        sendPacket(connectAcknowledge.getNetROMPacket());

        // Now we are ready to accept the connection and let it send it's i frames.
        if (chosen == null) {
            // We don't have a service for this callsign, so we will refuse the connection.
            circuit.setValid(false);
        } else {
            // We have a service for this callsign, so we will accept the connection and forward it to this service
            chosen.acceptedConnection(anInterface, user, circuit.getCircuitInputStream(), circuit.getCircuitOutputStream());
        }
    }


    /**
     * We have received a connection acknowledge
     *
     * @param connectAcknowledge
     * @throws IOException
     */
    public void receiveConnectionAcknowledge(ConnectAcknowledge connectAcknowledge) throws IOException {
        Circuit circuit = CircuitManager.getCircuit(connectAcknowledge.getYourCircuitIndex(), connectAcknowledge.getYourCircuitID());
        if (circuit == null) {
            // We should not get here as there should always be a circuit at this point.
            LOG.error("Received a connection ack for a circuit that does not exist.");
            // We should try to disconnect the session at this point as it is obviously borked.
            //disconnectCircuit(connectAcknowledge.getYourCircuitIndex(), connectAcknowledge.getYourCircuitID());
        }

        // Set the circuit state to connected.
        if (connectAcknowledge.getOpcode() == NetROMPacket.OPCODE_CONNECT_ACK) {
            // Connection was accepted.
            circuit.setState(CircuitState.CONNECTED);
        } else {
            // Connection was refused.
            circuit.setState(CircuitState.DISCONNECTED);
        }
    }

    /**
     * We have received a disconnect request from a remote node so we will need to teardown both ends of the circuit.
     *
     * @param disconnectRequest
     */
    public void receiveDisconnectRequest(DisconnectRequest disconnectRequest) throws IOException {

        // Get the circuit
        Circuit circuit = CircuitManager.getCircuit(disconnectRequest.getYourCircuitIndex(), disconnectRequest.getYourCircuitID());
        if (circuit == null) {
            // We should not get here as there should always be a circuit at this point.
            LOG.error("Received a disconnect request for a circuit that does not exist.");
        } else {
            // This circuit ends at us, so we need to disconnect the other end.
            circuit.getCircuitOutputStream().close();
            circuit.getCircuitInputStream().close();
        }

        // Send the disconnect ack.
        DisconnectAcknowledge disconnectAcknowledge = new DisconnectAcknowledge();
        disconnectAcknowledge.setYourCircuitIndex(disconnectRequest.getYourCircuitIndex());
        disconnectAcknowledge.setYourCircuitID(disconnectRequest.getYourCircuitID());
        disconnectAcknowledge.setSourceCallsign(disconnectRequest.getDestinationCallsign());
        disconnectAcknowledge.setDestinationCallsign(disconnectRequest.getSourceCallsign());


        sendPacket(disconnectAcknowledge.getNetROMPacket());
    }


    /**
     * Receive a disconnect acknowledge.
     *
     * @param disconnectAcknowledge
     */
    public void receiveDisconnectAck(DisconnectAcknowledge disconnectAcknowledge) {

        // Get the circuit
        Circuit circuit = CircuitManager.getCircuit(disconnectAcknowledge.getYourCircuitIndex(), disconnectAcknowledge.getYourCircuitID());
        circuit.setState(CircuitState.DISCONNECTED);

        // TODO: Cancel any retry timers

        // TODO: Let the originating client know disconnection is complete.
    }

    /**
     * Receive information frame - this could be out of order, so we need to buffer it
     */
    public void receiveInformationTransfer(Information information) throws IOException {
        Circuit circuit = CircuitManager.getCircuit(information.getYourCircuitIndex(), information.getYourCircuitID());

        if (circuit == null) {
            // We're not connected (maybe we were restart mid-connection)
            // Possibly send a reset here.
            circuit = new Circuit();
            circuit.setSourceCallsign(information.getSourceCallsign());
            circuit.setDestinationCallsign(information.getDestinationCallsign());
            circuit.setYourCiruitID(information.getYourCircuitID());
            circuit.setYourCircuitIndex(information.getYourCircuitIndex());
            disconnectCircuit(circuit);
        }


        // First, add the frame to our received list.
        circuit.addReceviedFrame(information);

        // See if the remote is asking for any retransmissions
        if (information.isNakFlag()) {
            int frameToRetransmit = information.getRxSequenceNumber();
            Information toRetransmit = circuit.getSentFrame(frameToRetransmit);
            sendPacket(toRetransmit.getNetROMPacket());
        }

        // Set the choke state.
        circuit.setChoked(information.isChokeFlag());

    }

    /**
     * Receive an information ack
     */
    public void receiveInformationAcknowledge(InformationAcknowledge informationAcknowledge) {

        Circuit circuit = CircuitManager.getCircuit(informationAcknowledge.getYourCircuitIndex(), informationAcknowledge.getYourCircuitID());

        if (circuit == null) {
            LOG.debug("Received an information ack for a circuit that does not exist.");
            return;
        }

        circuit.processAck(informationAcknowledge);
    }

    /**
     * Initiate a disconnect on a circuit.
     */
    public void disconnectCircuit(Circuit circuit) throws IOException {
        // Send a disconnect request.
        DisconnectRequest disconnectRequest = new DisconnectRequest();
        disconnectRequest.setSourceCallsign(circuit.getDestinationCallsign());
        disconnectRequest.setDestinationCallsign(circuit.getSourceCallsign());
        disconnectRequest.setYourCircuitIndex(circuit.getYourCircuitIndex());
        disconnectRequest.setYourCircuitID(circuit.getYourCircuitID());

        circuit.setState(CircuitState.DISCONNECTING);

        // TODO: Implement some ACK timeout thing here to resend X times until an ACK is received.

        sendPacket(disconnectRequest.getNetROMPacket());
    }

    /**
     * Find a node that's closer than us and forward the packet to it.
     *
     * @param packet
     * @throws IOException
     */
    public void forwardPacket(NetROMPacket packet) throws IOException {
        // Get the next hop
        NetROMRoute route = NetROMRoutingTable.INSTANCE.getRoutingToCallsign(packet.getDestinationCallsign().toString());
        if (route != null && !route.isExipred()) {
            // Get the client handler or connect to the node if we don't have a connection.
            NetROMClientHandler nextStation = service.getClientHandlerForCallsign(route.getAnInterface(), new AX25Callsign(route.getSourceCallsign()), true);
            if (nextStation != null) {
                // Forward the packet if the TTL is still valid.
                if (packet.decrementTTL() > 0) {
                    nextStation.sendPacket(packet);
                }
            } else {
                // Failed to send packet as no node connected!
            }
        }
    }

    /**
     * Send a packet to the remote node.
     * <p>
     * TODO: Make this a queue that is processed by a thread, and checks the choke flag to hold packets.
     *
     * @param packet
     * @throws IOException
     */
    public synchronized void sendPacket(NetROMPacket packet) throws IOException {
        // Send the packet.
        out.write(packet.toPacket());
        out.flush();
    }

    /**
     * This is the remote node that we are connected to
     *
     * @return
     */
    public User getUser() {
        return user;
    }


//    /**
//     * Send a connection request, wait for a reply or timeout.
//     *
//     * @param sourceCircuit the circuit we are making the connection for.
//     */
//    private Circuit sendConnectionRequestBlocking(ClientHandler sourceHandler, Circuit sourceCircuit) {
//
//        // Create a circuit to handle this connection request
//        Circuit circuit = new Circuit();
//        circuit.setSourceCallsign(sourceCircuit.getSourceCallsign());
//        circuit.setDestinationCallsign(sourceCircuit.getDestinationCallsign());
//        circuit.setOriginatingUser(sourceCircuit.getOriginatingUser());
//        circuit.setOriginatingNode(sourceCircuit.getOriginatingNode());
//        circuit.setAcceptedSize(sourceCircuit.getAcceptedSize());
//
//        CircuitManager.registerCircuit(circuit, this); // Applies the circuit IDs and indexes.
//        circuit.setOtherCircuit(sourceCircuit);
//        sourceCircuit.setOtherCircuit(circuit);
//
//        // Setup a connection request with the relevant infos.
//        ConnectRequest connectRequest = new ConnectRequest();
//        connectRequest.setMyCircuitIndex(sourceCircuit.getMyCircuitIndex());
//        connectRequest.setMyCircuitID(sourceCircuit.getMyCircuitId());
//        connectRequest.setProposeWindowSize(sourceCircuit.getAcceptedSize());
//        connectRequest.setDestinationCallsign(sourceCircuit.getDestinationCallsign());
//        connectRequest.setSourceCallsign(sourceCircuit.getSourceCallsign());
//        connectRequest.getNetROMPacket().setOriginCallsign(sourceCircuit.getSourceCallsign().toString());
//        connectRequest.getNetROMPacket().setDestinationCallsign(sourceCircuit.getDestinationCallsign().toString());
//
//        try {
//            // Send the connection request and wait for a reply.
//            sendPacket(connectRequest.getNetROMPacket());
//
//            // Wait for a connection ack or nack to appear from the node we are talking to
//            waitForConnectionAck(sourceHandler, circuit);
//        } catch (IOException e) {
//            LOG.debug(e.getMessage(), e);
//            circuit.setValid(false);
//        }
//
//        // Set this circuit state
//        circuit.setState(circuit.isValid() ? CircuitState.CONNECTED : CircuitState.DISCONNECTED);
//
//        return circuit;
//    }

//    /**
//     * Waits for reception of a connection ack/nack or a timeout.
//     *
//     * @param sourceHandler
//     */
//    public Circuit waitForConnectionAck(ClientHandler sourceHandler, Circuit connectingCircuit) {
//        // Wait for a connection ack or nack to appear from the node we are talking to
//        long startTime = System.currentTimeMillis();
//        while (System.currentTimeMillis() - startTime < TIMEOUT) {
//            Tools.delay(100); // FIXME: remove this and use a proper wait/notify
//            // Check for a connection ack/nack
//            if (connectingCircuit == null || (connectingCircuit.getState() == CircuitState.CONNECTED || connectingCircuit.getState() == CircuitState.DISCONNECTED)) {
//                // We have a connection ack or the circuit failed to setup.
//                break;
//            }
//        }
//        return connectingCircuit;
//    }


}