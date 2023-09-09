package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.kisset.protocols.netrom.NetROMPacket;

/**
 * Undocumented reset - operation inferred from linux sources.
 */
public class Reset {

    private NetROMPacket netROMPacket;

    public Reset(NetROMPacket packet) {
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
