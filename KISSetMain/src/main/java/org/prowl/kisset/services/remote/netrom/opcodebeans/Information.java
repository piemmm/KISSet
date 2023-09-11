package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class Information {

    private NetROMPacket netROMPacket;

    public Information(NetROMPacket packet) {
        this.netROMPacket = packet;
    }

    public int getYourCircuitIndex() {
        return netROMPacket.getCircuitIndex();
    }

    public int getYourCircuitID() {
        return netROMPacket.getCircuitId();
    }

    public int getTxSequenceNumber() {
        return netROMPacket.getTxSequenceNumber();
    }

    public int getRxSequenceNumber() {
        return netROMPacket.getRxSequenceNumber();
    }

    public int getOpCode() {
        return netROMPacket.getOpCode();
    }

    public byte[] getBody() {
        return netROMPacket.getBody();
    }


    public AX25Callsign getSourceCallsign() {
        return new AX25Callsign(netROMPacket.getOriginCallsign());
    }

    public AX25Callsign getDestinationCallsign() {
        return new AX25Callsign(netROMPacket.getDestinationCallsign());
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

    /**
     * If set, it indicates that the information is a fragment of a long
     * information frame and must be reassembled with one or more following information
     * messages by the destination node.
     * @return
     */
    public boolean isMoreFollowsFlag() {
        return netROMPacket.isMoreFollowsFlag();
    }

    public void setBody(byte[] body) {
        netROMPacket.setBody(body);
    }

    public NetROMPacket getNetROMPacket() {
        return netROMPacket;
    }

}
