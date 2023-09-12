package org.prowl.kisset.services.remote.netrom.circuit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.services.remote.netrom.server.NetROMClientHandler;
import org.prowl.kisset.services.remote.netrom.opcodebeans.Information;
import org.prowl.kisset.services.remote.netrom.opcodebeans.InformationAcknowledge;
import org.prowl.kisset.util.PipedIOStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A circuit represents a connection from a node to node
 * <p>
 * A circuit is identified by a circuit index and a circuit id.
 */
public class Circuit {

    private static final Log LOG = LogFactory.getLog("Circuit");


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

    private boolean choke = false; // If true, we have been choked by the remote end - don't send any data.

    // IO streams (if this circuit terminates at us) - if we are forwarding, these are null.
    private PipedIOStream circuitInputStream = new PipedIOStream();
    private PipedIOStream circuitOutputStream = new PipedIOStream() {
        @Override
        public void flush() throws IOException {
            if (ownerClientHandler != null) {

                // Read up to window size
                int len = Math.min(acceptedSize, available());
                byte[] data = new byte[len];
                circuitOutputStream.read(data);

                Information information = new Information();
                information.setSourceCallsign(destinationCallsign);
                information.setDestinationCallsign(sourceCallsign);
                information.setYourCircuitIndex(yourCircuitIndex);
                information.setYourCircuitID(yourCiruitID);
                information.setTxSequenceNumber(txSequenceNumber);
                information.setRxSequenceNumber(rxSequenceNumber);
                information.setBody(data);

                incrementTxSequenceNumber();

                ownerClientHandler.sendPacket(information.getNetROMPacket());
            }
        }
    };

    private NetROMClientHandler ownerClientHandler; // The current owner (until a route changes)

    private boolean isValid = true; // This is false if the circuit could not be registered.

    // Sequence numbers allow up to 127 frames.
    private int txSequenceNumber = 0; // We have sent
    private int txSequenceNumberAck = 0; // We have received an ACK for

    private int rxSequenceNumber = 0; // We have received



    private final Object MONITOR = new Object();
    private Timer ackTimer;

    /**
     * Information frames are stored here until they are ACKed (for tx) and until we have received all frames in a sequence (for rx)
     */
    private Map<Integer, Information> incomingInformationFrames = new HashMap<>();
    private Map<Integer, Information> outgoingInformationFrames = new HashMap<>();

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


    /**
     * Add a received information frame.
     *
     * @param information
     */
    public void addReceviedFrame(Information information) {
        incomingInformationFrames.put(information.getRxSequenceNumber(), information);

        if (!information.isNakFlag()) {
            // Update the tx sequence number ack if the rx (piggybacked acknowledge it is actually tx ack) sequence number is greater than the current ack.
            // Bear in mind that this can loop around to 0 after passing 127
            if (txSequenceNumberAck < information.getRxSequenceNumber()) {
                txSequenceNumberAck = information.getRxSequenceNumber();
            }
            // If the tx sequence number is not the same as the rx (piggybacked acknowledge)) then we *might* need
            // to retransmit a frame.
            if (txSequenceNumber != information.getRxSequenceNumber()) {

            }

        }


        // Reassemble any our of order frames.
        checkReassembly(information);
    }




    public void checkReassembly(Information information) {
        synchronized(MONITOR) {
            // Get all the frames we have so far
            int count = 0;
            while (incomingInformationFrames.get(rxSequenceNumber + count) != null) {
                Information toReassemble = incomingInformationFrames.remove(rxSequenceNumber);
                byte[] data = toReassemble.getBody();
                for (int i = 0; i < data.length; i++) {
                    try {
                        writeByte(data[i] & 0xFF);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                count++;

                // Delayed queue for an ACK packet as we don't really need to send one for every single frame received,
                // Just the most recent one will do to save on traffic.
                queueAck(information);
            }
            rxSequenceNumber = count - 1;
        }
    }

    /**
     * Queues and ACK frame for information - this is delayed by 2 seconds to allow for more frames to be received.
     */
    public void queueAck(Information information) {
        synchronized(MONITOR) {
            if (ackTimer != null) {
                ackTimer.cancel();
            }

            ackTimer = new Timer();
            ackTimer.schedule(new TimerTask() {
                private Timer original = ackTimer;
                @Override
                public void run() {
                    synchronized (MONITOR) {
                        if (ackTimer != original) {
                            return;
                        }

                        // No ownerClientHandler? Then this is an erroneous frame from a previous app instance
                        if (isValid && ownerClientHandler != null) {
                            try {
                                InformationAcknowledge ack = new InformationAcknowledge();
                                ack.setSourceCallsign(information.getDestinationCallsign());
                                ack.setDestinationCallsign(information.getSourceCallsign());
                                ack.setYourCircuitIndex(yourCircuitIndex);
                                ack.setYourCircuitID(yourCiruitID);
                                ack.setRxSequenceNumber(rxSequenceNumber);
                                ownerClientHandler.sendPacket(ack.getNetROMPacket());
                            } catch (IOException e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            }, 2000);

        }
    }

    /**
     * Store a sent information frame incase we are in need of a retransmit.
     *
     * @param information
     */
    public void addSentFrame(Information information) {
        outgoingInformationFrames.put(information.getTxSequenceNumber(), information);
    }

    public Information getSentFrame(int txSequenceNumber) {
        return outgoingInformationFrames.get(txSequenceNumber);
    }

    public boolean isRxChoked() {
        return choke;
    }

    public void setChoked(boolean choke) {
        this.choke = choke;
    }

}
