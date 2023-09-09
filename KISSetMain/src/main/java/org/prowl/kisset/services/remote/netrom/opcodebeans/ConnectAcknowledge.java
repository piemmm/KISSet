package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class ConnectAcknowledge {

    private NetROMPacket netROMPacket;

    public ConnectAcknowledge(NetROMPacket packet) {
        this.netROMPacket = packet;
    }

    public ConnectAcknowledge() {
        this.netROMPacket = new NetROMPacket();
        netROMPacket.getBody()[0] = 100;
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

    public int getMyCircuitIndex() {
        return netROMPacket.getTxSequenceNumber();
    }

    public void setMyCircuitIndex(int circuitIndex) {
        netROMPacket.setTxSequenceNumber(circuitIndex);
    }

    public int getMyCircuitID() {
        return netROMPacket.getRxSequenceNumber();
    }

    public void setMyCircuitID(int circuitID) {
        netROMPacket.setRxSequenceNumber(circuitID);
    }

    public int getOpcode() {
        return netROMPacket.getOpCode();
    }

    public void setOpcode(int opcode) {
        netROMPacket.setOpCode(opcode);
    }

    public int getAcceptWindowSize() {
        return netROMPacket.getBody()[0];
    }

    public void setAcceptWindowSize(int windowSize) {
        netROMPacket.getBody()[0] = (byte) windowSize;
    }

    public NetROMPacket getNetROMPacket() {
        return netROMPacket;
    }


    public AX25Callsign getSourceCallsign() {
        return new AX25Callsign(netROMPacket.getOriginCallsign());
    }

    public AX25Callsign getDestinationCallsign() {
        return new AX25Callsign(netROMPacket.getDestinationCallsign());
    }

    /**
     * If the high order bit of the opcode is set then it means the connection was refused.
     * @return
     */
    public boolean isRefused() {
        return netROMPacket.getOpCode() > 127;
    }

}
