package org.prowl.kisset.services.remote.netrom.circuit;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.services.ClientHandler;
import org.prowl.kisset.services.remote.netrom.NetROMClientHandler;
import org.prowl.kisset.util.PipedIOStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A circuit represents a connection from a node to node
 * <p>
 * A circuit is identified by a circuit index and a circuit id.
 */
public class Circuit {

    private AX25Callsign sourceCallsign;
    private AX25Callsign destinationCallsign;

    private int myCircuitIndex;
    private int myCircuitId;
    private int yourCircuitIndex;
    private int yourCiruitID;
    private CircuitState state;
    private int acceptedSize;
    private AX25Callsign originatingUser;
    private AX25Callsign originatingNode;

    // IO streams (if this circuit terminates at us) - if we are forwarding, these are null.
    private PipedIOStream circuitInputStream = new PipedIOStream();
    private PipedIOStream circuitOutputStream = new PipedIOStream();

    private NetROMClientHandler ownerClientHandler;
    // The other circuit if we are forwarding.
    private Circuit otherCircuit;

    private boolean terminatesLocally = false;
    private boolean isValid = true; // This is false if the circuit could not be registered.

    // Sequence numbers allow up to 127 frames.
    private int txSequenceNumber = 0;
    private int rxSequenceNumber = 0;

    public Circuit(int myCircuitIndex, int myCircuitId) {
        this.myCircuitIndex = myCircuitIndex;
        this.myCircuitId = myCircuitId;
        state = CircuitState.DISCONNECTED;
    }

    public Circuit() {
        isValid = false;
        state = CircuitState.DISCONNECTED;
    }

    public void setYourCircuitInfo(int yourCircuitIndex, int yourCircuitId) {
        this.yourCircuitIndex = yourCircuitIndex;
        this.yourCiruitID = yourCircuitId;
    }

    public int getMyCircuitIndex() {
        return myCircuitIndex;
    }

    public void setMyCircuitIndex(int myCircuitIndex) {
        this.myCircuitIndex = myCircuitIndex;
    }

    public int getMyCircuitId() {
        return myCircuitId;
    }

    public void setMyCircuitId(int myCircuitId) {
        this.myCircuitId = myCircuitId;
    }

    public int getYourCircuitIndex() {
        return yourCircuitIndex;
    }

    public void setYourCircuitIndex(int yourCircuitIndex) {
        this.yourCircuitIndex = yourCircuitIndex;
    }

    public int getYourCircuitID() {
        return yourCiruitID;
    }

    public void setYourCiruitID(int yourCiruitID) {
        this.yourCiruitID = yourCiruitID;
    }

    public CircuitState getState() {
        return state;
    }

    public void setState(CircuitState state) {
        this.state = state;
    }

    public int getAcceptedSize() {
        return acceptedSize;
    }

    public void setAcceptedSize(int acceptedSize) {
        this.acceptedSize = acceptedSize;
    }

    public AX25Callsign getOriginatingUser() {
        return originatingUser;
    }

    public void setOriginatingUser(AX25Callsign originatingUser) {
        this.originatingUser = originatingUser;
    }

    public AX25Callsign getOriginatingNode() {
        return originatingNode;
    }

    public void setOriginatingNode(AX25Callsign originatingNode) {
        this.originatingNode = originatingNode;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public AX25Callsign getSourceCallsign() {
        return sourceCallsign;
    }

    public void setSourceCallsign(AX25Callsign sourceCallsign) {
        this.sourceCallsign = sourceCallsign;
    }

    public AX25Callsign getDestinationCallsign() {
        return destinationCallsign;
    }

    public void setDestinationCallsign(AX25Callsign destinationCallsign) {
        this.destinationCallsign = destinationCallsign;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Circuit: ");
        sb.append(" MyCircuitIndex: ");
        sb.append(myCircuitIndex);
        sb.append(" MyCircuitId: ");
        sb.append(myCircuitId);
        sb.append(" YourCircuitIndex: ");
        sb.append(yourCircuitIndex);
        sb.append(" YourCircuitId: ");
        sb.append(yourCiruitID);
        sb.append(" State: ");
        sb.append(state);
        sb.append(" AcceptedSize: ");
        sb.append(acceptedSize);
        sb.append(" OriginatingUser: ");
        sb.append(originatingUser);
        sb.append(" OriginatingNode: ");
        sb.append(originatingNode);
        sb.append(" IsValid: ");
        sb.append(isValid);
        sb.append(" SourceCallsign: ");
        sb.append(sourceCallsign);
        sb.append(" DestinationCallsign: ");
        sb.append(destinationCallsign);

        return sb.toString();
    }

    public NetROMClientHandler getOwnerClientHandler() {
        return ownerClientHandler;
    }

    public void setOwnerClientHandler(NetROMClientHandler ownerClientHandler) {
        this.ownerClientHandler = ownerClientHandler;
    }

    public Circuit getOtherCircuit() {
        return otherCircuit;
    }

    public void setOtherCircuit(Circuit otherCircuit) {
        this.otherCircuit = otherCircuit;
    }

    public InputStream getCircuitInputStream() {
        return circuitInputStream;
    }


    public OutputStream getCircuitOutputStream() {
        return circuitOutputStream.getOutputStream();
    }

    public void writeByte(int b) throws IOException {
        circuitInputStream.getOutputStream().write(b);
    }

    public int readByte() throws IOException {
        return circuitOutputStream.read();
    }

    /**
     * True if the endpoint for this circuit is at this node
     * @return
     */
    public boolean isTerminatesLocally() {
        return terminatesLocally;
    }

    /**
     * Set to true if this circuit does not forward to another circuit, but instead terminates at a service or user on this node
     * @param terminatesLocally
     */
    public void setTerminatesLocally(boolean terminatesLocally) {
        this.terminatesLocally = terminatesLocally;
    }

public int getTxSequenceNumber() {
        return txSequenceNumber;
    }

    public void setTxSequenceNumber(int txSequenceNumber) {
        this.txSequenceNumber = txSequenceNumber;
    }

    public int getRxSequenceNumber() {
        return rxSequenceNumber;
    }

    public void setRxSequenceNumber(int rxSequenceNumber) {
        this.rxSequenceNumber = rxSequenceNumber;
    }

    public void incrementTxSequenceNumber() {
        txSequenceNumber++;
        if (txSequenceNumber > 127) {
            txSequenceNumber = 0;
        }
    }

    public void incrementRxSequenceNumber() {
        rxSequenceNumber++;
        if (rxSequenceNumber > 127) {
            rxSequenceNumber = 0;
        }
    }



}
