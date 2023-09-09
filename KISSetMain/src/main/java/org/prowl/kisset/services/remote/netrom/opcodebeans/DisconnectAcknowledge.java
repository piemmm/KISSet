package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class DisconnectAcknowledge {

    private NetROMPacket netROMPacket;

    public DisconnectAcknowledge(NetROMPacket packet) {
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


    public AX25Callsign getSourceCallsign() {
        return new AX25Callsign(netROMPacket.getOriginCallsign());
    }

    public AX25Callsign getDestinationCallsign() {
        return new AX25Callsign(netROMPacket.getDestinationCallsign());
    }

}
