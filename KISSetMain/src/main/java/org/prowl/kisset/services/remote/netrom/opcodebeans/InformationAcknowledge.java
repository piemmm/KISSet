package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class InformationAcknowledge {

    private NetROMPacket netROMPacket;

    public InformationAcknowledge(NetROMPacket packet) {
        this.netROMPacket = packet;
    }

    public int getYourCircuitIndex() {
        return netROMPacket.getCircuitIndex();
    }

    public int getYourCircuitID() {
        return netROMPacket.getCircuitId();
    }

    public int getRxSequenceNumber() {
        return netROMPacket.getRxSequenceNumber();
    }

    public int getOpCode() {
        return netROMPacket.getOpCode();
    }

    /**
     * If set, it indicates that this node cannot accept any further
     * information messages until further notice.
     * @return
     */
    public boolean isChokeFlag() {
        return netROMPacket.isChokeFlag();
    }

    /**
     * If set, indicates a selective retransmission of the frame
     * identified by the Rx sequence number is being requested.
     * @return
     */
    public boolean isNakFlag() {
        return netROMPacket.isNakFlag();
    }


    public AX25Callsign getSourceCallsign() {
        return new AX25Callsign(netROMPacket.getOriginCallsign());
    }

    public AX25Callsign getDestinationCallsign() {
        return new AX25Callsign(netROMPacket.getDestinationCallsign());
    }


}
