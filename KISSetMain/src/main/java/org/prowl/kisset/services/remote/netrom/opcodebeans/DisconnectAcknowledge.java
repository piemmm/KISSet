package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class DisconnectAcknowledge {

    private NetROMPacket netROMPacket;

    public DisconnectAcknowledge(NetROMPacket packet) {
        this.netROMPacket = packet;
    }

    public DisconnectAcknowledge() {
        netROMPacket = new NetROMPacket();
        netROMPacket.setBody(new byte[0]);
        netROMPacket.setOpCode(NetROMPacket.OPCODE_DISCONNECT_ACK);
    }

    public int getYourCircuitIndex() {
        return netROMPacket.getCircuitIndex();
    }

    public int getYourCircuitID() {
        return netROMPacket.getCircuitId();
    }

    public int getOpcode() {
        return netROMPacket.getOpCode();
    }

    public void setYourCircuitIndex(int circuitIndex) {
        netROMPacket.setCircuitIndex(circuitIndex);
    }

    public void setYourCircuitID(int circuitID) {
        netROMPacket.setCircuitId(circuitID);
    }

    public AX25Callsign getSourceCallsign() {
        return new AX25Callsign(netROMPacket.getOriginCallsign());
    }

    public AX25Callsign getDestinationCallsign() {
        return new AX25Callsign(netROMPacket.getDestinationCallsign());
    }

    public void setSourceCallsign(AX25Callsign callsign) {
        netROMPacket.setOriginCallsign(callsign.toString());
    }

    public void setDestinationCallsign(AX25Callsign callsign) {
        netROMPacket.setDestinationCallsign(callsign.toString());
    }

    public NetROMPacket getNetROMPacket() {
        return netROMPacket;
    }
}
