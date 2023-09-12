package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class InformationAcknowledge {

    private NetROMPacket netROMPacket;

    public InformationAcknowledge(NetROMPacket packet) {
        this.netROMPacket = packet;
    }

    public InformationAcknowledge() {
        netROMPacket = new NetROMPacket();
        netROMPacket.setOpCode(NetROMPacket.OPCODE_INFORMATION_ACK);
        netROMPacket.setBody(new byte[0]);
    }

    public int getYourCircuitIndex() {
        return netROMPacket.getCircuitIndex();
    }

    public void setYourCircuitIndex(int circuitIndex) {
        netROMPacket.setCircuitIndex(circuitIndex);
    }

    public int getYourCircuitID() {
        return netROMPacket.getCircuitId();
    }

    public void setYourCircuitID(int circuitID) {
        netROMPacket.setCircuitId(circuitID);
    }

    public int getRxSequenceNumber() {
        return netROMPacket.getRxSequenceNumber();
    }

    public void setRxSequenceNumber(int rxSequenceNumber) {
        netROMPacket.setRxSequenceNumber(rxSequenceNumber);
    }


    public int getOpCode() {
        return netROMPacket.getOpCode();
    }


    /**
     * If set, it indicates that this node cannot accept any further
     * information messages until further notice.
     *
     * @return
     */
    public boolean isChokeFlag() {
        return netROMPacket.isChokeFlag();
    }

    public void setChokeFlag(boolean choke) {
        netROMPacket.setChokeFlag(choke);
    }

    /**
     * If set, indicates a selective retransmission of the frame
     * identified by the Rx sequence number is being requested.
     *
     * @return
     */
    public boolean isNakFlag() {
        return netROMPacket.isNakFlag();
    }

    public void setNakFlag(boolean nak) {
        netROMPacket.setNakFlag(nak);
    }

    public AX25Callsign getSourceCallsign() {
        return new AX25Callsign(netROMPacket.getOriginCallsign());
    }

    public void setSourceCallsign(AX25Callsign callsign) {
        netROMPacket.setOriginCallsign(callsign.toString());
    }

    public AX25Callsign getDestinationCallsign() {
        return new AX25Callsign(netROMPacket.getDestinationCallsign());
    }

    public void setDestinationCallsign(AX25Callsign callsign) {
        netROMPacket.setDestinationCallsign(callsign.toString());
    }

    public NetROMPacket getNetROMPacket() {
        return netROMPacket;
    }
}
