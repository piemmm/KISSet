package org.prowl.kisset.protocols.core;


import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.io.Interface;

import java.util.ArrayList;
import java.util.List;

/**
 * Holder that represents another node
 *
 * @return
 */
public class Node {

    /**
     * Reference to the connector the device was heard on
     */
    private Interface anInterface;

    /**
     * The node callsign
     */
    private String callsign;

    /**
     * The time it was last heard
     */
    private long lastHeard;

    /**
     * Destination alias/callsign/ui/etc
     */
    private String destination;

    /**
     * A list of capabilities this node has been seen to advertise or use
     */
    private List<Capability> capabilities = new ArrayList<>();

    /**
     * A list of callsigns that have been seen to be able to converse with this node
     */
    private List<Node> canReach = new ArrayList<>();

    /**
     * Actual frame received - may be null if not applicable
     */
    private AX25Frame frame;
    /**
     * The signal strength (if applicable), 0 if not.
     */
    private double rssi = Double.MAX_VALUE;


    public Node(Interface anInterface, String callsign, long lastHeard, double rssi, String destination) {
        this.callsign = callsign;
        this.lastHeard = lastHeard;
        this.rssi = rssi;
        this.anInterface = anInterface;
        this.destination = destination;
    }

    /**
     * Create a node object, no signal strength information present
     *
     * @param anInterface
     * @param callsign
     * @param lastHeard
     */
    public Node(Interface anInterface, String callsign, long lastHeard, String destination, AX25Frame frame) {
        this.callsign = callsign;
        this.lastHeard = lastHeard;
        this.anInterface = anInterface;
        this.destination = destination;
        this.frame = frame;
    }

    /**
     * Create a copy of the supplied node.
     *
     * @param toCopy
     */
    public Node(Node toCopy) {
        this.callsign = toCopy.callsign;
        this.lastHeard = toCopy.lastHeard;
        this.rssi = toCopy.rssi;
        this.anInterface = toCopy.getInterface();
        this.capabilities = new ArrayList<>(toCopy.capabilities);
        this.destination = toCopy.destination;
    }

    /**
     * Add a capability if it's not there, otherwise update the last seen.
     *
     * @param capability
     */
    public void addCapabilityOrUpdate(Capability capability) {
        for (Capability cap : capabilities) {
            if (cap.getService() == capability.getService()) {
                cap.setLastSeen(capability.getLastSeen());
                return;
            }
        }
        capabilities.add(capability);
    }

    /**
     * Add a node that has been seen to be able to reach this node and converse with it
     *
     * @param node The node that can reach this one
     */
    public void addCanReachNodeOrReplace(Node node) {
        canReach.remove(node);
        canReach.add(node);
    }

    /**
     * @return a copy of the array list containing current nodes that can reach this one
     */
    public List<Node> getCanReachNodes() {
        return new ArrayList<>(canReach);
    }

    /**
     * @return a copy of the array list containing current capabilities
     */
    public List<Capability> getCapabilities() {
        return new ArrayList<>(capabilities);
    }

    public String getCallsign() {
        return callsign;
    }

    public long getLastHeard() {
        return lastHeard;
    }

    public void setLastHeard(long lastHeard) {
        this.lastHeard = lastHeard;
    }

    public double getRSSI() {
        return rssi;
    }

    public Interface getInterface() {
        return anInterface;
    }

    public void setAnInterface(Interface anInterface) {
        this.anInterface = anInterface;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public String getDestination() {
        return destination;
    }

    public AX25Frame getFrame() {
        return frame;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((callsign == null) ? 0 : callsign.hashCode());
        result = prime * result + ((anInterface == null) ? 0 : anInterface.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        if (callsign == null) {
            if (other.callsign != null)
                return false;
        } else if (!callsign.equals(other.callsign))
            return false;
        if (anInterface == null) {
            if (other.anInterface != null)
                return false;
        } else if (!anInterface.equals(other.anInterface))
            return false;
        return true;
    }

    /**
     * Enum represenging the station type
     */
    public enum Service {
        BBS("BBS"),
        NETROM("NET/ROM"),
        APRS("APRS"), // APRS transmits are NOLVL3 as well.
        FLEXNET("FLEXNET"),
        OPENTRAC("OPENTRAC"),
        TEXNET("TEXNET"),
        NOLVL3("UI"), // Generic station
        VJ_IP("VJ-TCP/IP"),

        IP("TCP/IP"); // TCP/IP
        private String name;

        Service(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }


}
