package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class ConnectAcknowledge {

    private NetROMPacket netROMPacket;

    public ConnectAcknowledge(NetROMPacket packet) {
        this.netROMPacket = packet;
    }

    public int getYourCircuitIndex() {
        return netROMPacket.getCircuitIndex();
    }

    public int getYourCircuitID() {
        return netROMPacket.getCircuitId();
    }

    public int getMyCircuitIndex() {
        return netROMPacket.getTxSequenceNumber();
    }

    public int getMyCircuitID() {
        return netROMPacket.getRxSequenceNumber();
    }

    public int getOpcode() {
        return netROMPacket.getOpCode();
    }

    public int getAcceptWindowSize() {
        return netROMPacket.getBody()[0];
    }

    /**
     * If the high order bit of the opcode is set then it means the connection was refused.
     * @return
     */
    public boolean isRefused() {
        return netROMPacket.getOpCode() > 127;
    }

}
