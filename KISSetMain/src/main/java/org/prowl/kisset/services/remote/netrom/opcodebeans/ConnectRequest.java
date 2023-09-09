package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class ConnectRequest {

    private NetROMPacket netROMPacket;

    public ConnectRequest(NetROMPacket packet) {
        this.netROMPacket = packet;
    }

    public int getMyCircuitIndex() {
        return netROMPacket.getCircuitIndex();
    }

    public int getMyCircuitID() {
        return netROMPacket.getCircuitId();
    }

    public int getOpcode() {
        return netROMPacket.getOpCode();
    }

    public int getProposeWindowSize() {
        return netROMPacket.getBody()[0];
    }

    public AX25Callsign getSourceCallsign() {
        return new AX25Callsign(netROMPacket.getOriginCallsign());
    }

    public AX25Callsign getDestinationCallsign() {
        return new AX25Callsign(netROMPacket.getDestinationCallsign());
    }

    public AX25Callsign getCallsignOfOriginatingUser() {
        return new AX25Callsign(netROMPacket.getBody(),7,7);
    }

    public AX25Callsign getCallsignOfOriginatingNode() {
        return new AX25Callsign(netROMPacket.getBody(),7+7,7);
    }

}
