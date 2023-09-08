package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class DisconnectRequest {

    private NetROMPacket netROMPacket;

    public DisconnectRequest(NetROMPacket packet) {
        this.netROMPacket = packet;
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
}
