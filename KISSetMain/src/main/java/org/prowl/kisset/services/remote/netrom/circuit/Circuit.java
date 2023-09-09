package org.prowl.kisset.services.remote.netrom.circuit;

import org.prowl.ax25.AX25Callsign;

/**
 * A circuit represents a connection from a node to node
 *
 * A circuit is identified by a circuit index and a circuit id.
 */
public class Circuit {

    private int myCircuitIndex;
    private int myCircuitId;
    private int yourCircuitIndex;
    private int yourCiruitID;
    private CircuitState state;
    private int acceptedSize;
    private AX25Callsign originatingUser;
    private AX25Callsign originatingNode;

    private boolean isValid = true; // This is false if the circuit could not be registered.

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

    void setValid(boolean isValid) {
        this.isValid = isValid;
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
        return sb.toString();
    }
}
