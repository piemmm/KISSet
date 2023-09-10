package org.prowl.kisset.services.remote.netrom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.routing.NetROMRoute;
import org.prowl.kisset.objects.user.User;
import org.prowl.kisset.protocols.netrom.NetROMPacket;
import org.prowl.kisset.protocols.netrom.NetROMRoutingTable;
import org.prowl.kisset.services.ClientHandler;
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

public class NetROMClientHandler implements ClientHandler {

    private static final Log LOG = LogFactory.getLog("NetROMClientHandler");

    private static final String CR = "\r";

    private static final long TIMEOUT = 120000; // 2 minutes

    private InputStream in;
    private OutputStream out;
    private final User user;
    private final Interface anInterface;
    private boolean colourEnabled = true;
    private BufferedReader bin;
    private NetROMService service;

    public NetROMClientHandler(NetROMService service, Interface anInterface, User user, InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.user = user;
        this.service = service;
        this.anInterface = anInterface;

    }

    @Override
    /**
     * Read in packets from our connection and process them.
     */ public void start() {

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

                if (data.length > 0) {
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
    }


    /**
     * Process a netrom packet from a connected node according to the netrom spec.
     * <p>
     * A connected node could be conversing with us about several different circuits/things
     * so we will farm each of these off to their own individual handlers where needed.
     *
     * @param packet
     */
    public void processPacket(NetROMPacket packet) throws IOException {

        // We have a packet, let's process it.
        // This may need to be split off into some form of thread based processing so we can handle multiple circuits which
        // might need to do blocking stuff on paticular requests, without stalling the rest of the connection
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

                break;
            case NetROMPacket.OPCODE_INFORMATION_ACK:
                // We have a data ack.
                LOG.debug("Got a data ack from " + packet.getOriginCallsign() + " to " + packet.getDestinationCallsign());
                InformationAcknowledge informationAcknowledge = new InformationAcknowledge(packet);

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

        Circuit circuit = new Circuit();
        circuit.setSourceCallsign(connectRequest.getSourceCallsign());
        circuit.setDestinationCallsign(connectRequest.getDestinationCallsign());
        circuit.setOriginatingUser(connectRequest.getCallsignOfOriginatingUser());
        circuit.setOriginatingNode(connectRequest.getCallsignOfOriginatingNode());
        circuit.setAcceptedSize(connectRequest.getProposeWindowSize());
        circuit.setYourCircuitIndex(connectRequest.getMyCircuitIndex());
        circuit.setYourCiruitID(connectRequest.getMyCircuitID());

        // if the connection is to us, then we need to pass it on to the relevant handler.
        if (connectRequest.getDestinationCallsign().toString().equalsIgnoreCase(service.getCallsign()) || connectRequest.getDestinationCallsign().toString().equalsIgnoreCase(service.getAlias())) {
            // Connection is to us.

            // Create the IO Streams for this connection
            InputStream circuitIn = new CircuitInputStream(this, circuit);
            OutputStream circuitOut = new CircuitOutputStream(this, circuit);

            circuit.setCircuitInputStream(circuitIn);
            circuit.setCircuitOutputStream(circuitOut);

            // Forward the connection to it's handler which is given the circuit input and circuit output streams.
            service.acceptedConnection(anInterface, user, circuitIn, circuitOut);

        } else {

            // Connection is to another node, not us.
            // Check we have a route to the destination node.
            NetROMRoute route = NetROMRoutingTable.INSTANCE.getRoutingToCallsign(circuit.getDestinationCallsign().toString());
            if (route != null && !route.isExipred()) {

                // See if we have an active connection to that node.
                NetROMClientHandler nextStation = service.getClientHandlerForCallsign(route.getAnInterface(), circuit.getDestinationCallsign(), true);
                if (nextStation != null) {
                    // We have a connection to the next station, so we can connect to the destination.

                    // Now register the new circuit with the CircuitManager
                    CircuitManager.registerCircuit(circuit);

                    // Set the circuit state to connected.
                    circuit.setState(CircuitState.CONNECTING);

                    // TODO: we now need to send a connection request to the next station and wait
                    //       for the reply, or timeout, and then send the connection ack/nack back
                    //       to the originating station.
                    Circuit remoteCircuit = nextStation.sendConnectionRequestBlocking(this, circuit);
                    if (remoteCircuit != null && remoteCircuit.isValid()) {
                        circuit.setState(circuit.isValid() ? CircuitState.CONNECTED : CircuitState.DISCONNECTED);
                    } else {
                        // Connection failed, so invalidate this circuit as well.
                        circuit.setValid(false);
                    }

                } else {
                    // No connection or were not able to connect
                    circuit.setValid(false);
                }
            } else {
                // We don't have a route to the destination, so we refuse the connection.
                circuit.setValid(false);// Reject the connection.
            }
        }

        // Send a connection ack/nack depending if the connection succeeded or failed
        ConnectAcknowledge connectAcknowledge = new ConnectAcknowledge();
        connectAcknowledge.setAcceptWindowSize(circuit.getAcceptedSize());
        connectAcknowledge.setYourCircuitIndex(circuit.getYourCircuitIndex());
        connectAcknowledge.setYourCircuitID(circuit.getYourCircuitID());
        connectAcknowledge.setMyCircuitIndex(circuit.getMyCircuitIndex());
        connectAcknowledge.setMyCircuitID(circuit.getMyCircuitId());
        // If we refused the connection then set the high order bit (which is in ACK_REFUSED for convenience)
        connectAcknowledge.setOpcode(circuit.isValid() ? NetROMPacket.OPCODE_CONNECT_ACK : NetROMPacket.OPCODE_CONNECT_ACK_REFUSED);

        // Send the packet.
        sendPacket(connectAcknowledge.getNetROMPacket());
    }

    /**
     * Send a connection request, wait for a reply or timeout.
     *
     * @param sourceCircuit the circuit we are making the connection for.
     */
    private Circuit sendConnectionRequestBlocking(ClientHandler sourceHandler, Circuit sourceCircuit) {

        // Create a circuit to handle this connection request
        Circuit circuit = new Circuit();
        circuit.setSourceCallsign(sourceCircuit.getSourceCallsign());
        circuit.setDestinationCallsign(sourceCircuit.getDestinationCallsign());
        circuit.setOriginatingUser(sourceCircuit.getOriginatingUser());
        circuit.setOriginatingNode(sourceCircuit.getOriginatingNode());
        circuit.setAcceptedSize(sourceCircuit.getAcceptedSize());
        CircuitManager.registerCircuit(circuit); // Applies the circuit IDs and indexes.

        // Setup a connection request with the relevant infos.
        ConnectRequest connectRequest = new ConnectRequest();
        connectRequest.setMyCircuitIndex(sourceCircuit.getMyCircuitIndex());
        connectRequest.setMyCircuitID(sourceCircuit.getMyCircuitId());
        connectRequest.setProposeWindowSize(sourceCircuit.getAcceptedSize());
        connectRequest.setDestinationCallsign(sourceCircuit.getDestinationCallsign());
        connectRequest.setSourceCallsign(sourceCircuit.getSourceCallsign());
        connectRequest.getNetROMPacket().setOriginCallsign(sourceCircuit.getSourceCallsign().toString());
        connectRequest.getNetROMPacket().setDestinationCallsign(sourceCircuit.getDestinationCallsign().toString());

        try {
            // Send the connection request and wait for a reply.
            sendPacket(connectRequest.getNetROMPacket());

            // Wait for a connection ack or nack to appear from the node we are talking to
            waitForConnectionAck(sourceHandler, circuit);
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
            circuit.setValid(false);
        }

        // Set this circuit state
        circuit.setState(circuit.isValid() ? CircuitState.CONNECTED : CircuitState.DISCONNECTED);

        return circuit;
    }

    /**
     * Waits for reception of a connection ack/nack or a timeout.
     *
     * @param sourceHandler
     */
    public Circuit waitForConnectionAck(ClientHandler sourceHandler, Circuit connectingCircuit) {
        // Wait for a connection ack or nack to appear from the node we are talking to
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < TIMEOUT) {
            Tools.delay(100); // FIXME: remove this and use a proper wait/notify
            // Check for a connection ack/nack
            if (connectingCircuit == null || (connectingCircuit.getState() == CircuitState.CONNECTED || connectingCircuit.getState() == CircuitState.DISCONNECTED)) {
                // We have a connection ack or the circuit failed to setup.
                break;
            }
        }
        return connectingCircuit;
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
        }

        // Are we forwarding between circuits or does this end at us?
        if (circuit.getDestinationCallsign().toString().equalsIgnoreCase(service.getCallsign()) || circuit.getDestinationCallsign().toString().equalsIgnoreCase(service.getAlias())) {
            // This circuit ends at us, so we need to disconnect the other end.
            circuit.getCircuitOutputStream().close();
            circuit.getCircuitInputStream().close();
        } else {
            // This circuit is forwarding, so we need to forward the disconnect reques to the other circuit.
            NetROMClientHandler otherCircuitClientHandler = circuit.getOtherCircuit().getOwnerClientHandler();
            otherCircuitClientHandler.disconnectCircuit(circuit);
        }

        // Send the disconnect ack.
        DisconnectAcknowledge disconnectAcknowledge = new DisconnectAcknowledge();
        disconnectAcknowledge.setYourCircuitIndex(disconnectRequest.getYourCircuitIndex());
        disconnectAcknowledge.setYourCircuitID(disconnectRequest.getYourCircuitID());


        sendPacket(disconnectAcknowledge.getNetROMPacket());
    }


    /**
     * Receive a disconnect acknowledge. Don't really know what the point of this is - once you send the disconnect
     * then the circuit is disconnected, there's not really much else to do.
     * @param disconnectAcknowledge
     */
    public void receiveDisconnectAck(DisconnectAcknowledge disconnectAcknowledge) {
       // Don't really care about this. There's no room in the spec for the other side to say 'no you can't disconnect'
       // so why does this ack even exist?
    }

    /**
     * Initiate a disconnect on a circuit.
     */
    public void disconnectCircuit(Circuit circuit) throws IOException {
        // Send a disconnect request.
        DisconnectRequest disconnectRequest = new DisconnectRequest();
        disconnectRequest.setYourCircuitIndex(circuit.getYourCircuitIndex());
        disconnectRequest.setYourCircuitID(circuit.getYourCircuitID());
        sendPacket(disconnectRequest.getNetROMPacket());
    }

    /**
     * Send a packet to the remote node.
     *
     * @param packet
     * @throws IOException
     */
    private void sendPacket(NetROMPacket packet) throws IOException {
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
}