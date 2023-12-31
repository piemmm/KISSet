package org.prowl.kisset.services.remote.netrom.opcodebeans;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.netrom.NetROMPacket;

public class ConnectRequest {

    private NetROMPacket netROMPacket;

    public ConnectRequest(NetROMPacket packet) {
        this.netROMPacket = packet;
    }

    public ConnectRequest() {
        this.netROMPacket = new NetROMPacket();
        this.netROMPacket.setBody(new byte[1+7+7]);
        this.netROMPacket.setOpCode(NetROMPacket.OPCODE_CONNECT_REQUEST);
    }

    public int getMyCircuitIndex() {
        return netROMPacket.getCircuitIndex();
    }

    public void setMyCircuitIndex(int circuitIndex) {
        netROMPacket.setCircuitIndex(circuitIndex);
    }

    public int getMyCircuitID() {
        return netROMPacket.getCircuitId();
    }

    public void setMyCircuitID(int circuitID) {
        netROMPacket.setCircuitId(circuitID);
    }


    public int getOpcode() {
        return netROMPacket.getOpCode();
    }

    public int getProposeWindowSize() {
        return netROMPacket.getBody()[0];
    }

    public void setProposeWindowSize(int windowSize) {
        netROMPacket.getBody()[0] = (byte) windowSize;
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


    public AX25Callsign getCallsignOfOriginatingUser() {
        return new AX25Callsign(netROMPacket.getBody(),1,7);
    }

    public AX25Callsign getCallsignOfOriginatingNode() {
        return new AX25Callsign(netROMPacket.getBody(),1+7,7);
    }

    public void setCallsignOfOriginatingUser(AX25Callsign callsign) {
        byte[] data = callsign.toByteArray();
        System.arraycopy(data, 0, netROMPacket.getBody(), 1, data.length);
    }

    public void setCallsignOfOriginatingNode(AX25Callsign callsign) {
        byte[] data = callsign.toByteArray();
        System.arraycopy(data, 0, netROMPacket.getBody(), 1 + 7, data.length);
    }

    public NetROMPacket getNetROMPacket() {
        return netROMPacket;
    }

    public int decrementTTL() {
        return netROMPacket.decrementTTL();
    }
}
