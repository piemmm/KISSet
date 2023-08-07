package org.prowl.kisset.netrom;

import org.prowl.kisset.ax25.AX25Callsign;
import org.prowl.kisset.ax25.AX25Frame;
import org.prowl.kisset.util.Tools;


public class NetROMPacket {

    //private static String body = "FF4D4E4B4E4F448E846E9A9C96644D4E4B4348548E846E9A9C9662FF8E846E9A9C96604D4E4B4242538E846E9A9C9662FF9A609C86AE40664352455343489A609C86AE4060BF8E846E9EAA96604F554B4E4F448E846E9EAA9660C08E846E9EAA96644F554B4348548E846E9EAA9660BF8E846E9EAA96664F554B4445568E846E9EAA9660BF9A846E9C98846042555A5A52449A846E9C988460C09A846E9C98846242555A4242539A846E9C988460969A846E9C98846442555A4348549A846E9C988460BF9A846E9C98846642555A5757439A846E9C988460BF";


    private byte[] netROMFrame;

    private AX25Callsign originCallsign;
    private AX25Callsign destinationCallsign;

    // Contents of the Net/ROM network and transport header
    private int ttl;
    private int circuitIndex;
    private int circuitId;
    private int txSequenceNumber;
    private int rxSequenceNumber;
    private int opCodeAndFlags; // see below

    // Contents of the opCodeAndFlags field
    private boolean chokeFlag;
    private boolean nakFlag;
    private boolean moreFollowsFlag;
    private boolean reserved;
    private int opCode;


    /**
     * Decode a Net/ROM broadcast packet from an AX.25 frame
     *
     * @param frame The AX.25 frame to decode
     */
    public NetROMPacket(AX25Frame frame) {
        netROMFrame = frame.body;

        // Decode the origin callsign and destination callsign
        originCallsign =frame.sender;
        destinationCallsign = frame.dest;

        // Decode the Net/ROM header
        ttl = netROMFrame[0] & 0xFF;
        circuitIndex = netROMFrame[1] & 0xFF;
        circuitId = netROMFrame[2] & 0xFF;
        txSequenceNumber = netROMFrame[3] & 0xFF;
        rxSequenceNumber = netROMFrame[4] & 0xFF;
        opCodeAndFlags = netROMFrame[5] & 0xFF;

        // Now decode the opcode and flags
        chokeFlag = (opCodeAndFlags & 0x80) != 0;
        nakFlag = (opCodeAndFlags & 0x40) != 0;
        moreFollowsFlag = (opCodeAndFlags & 0x20) != 0;
        reserved = (opCodeAndFlags & 0x10) != 0;
        opCode = opCodeAndFlags & 0x0F;
    }

    public int getTtl() {
        return ttl;
    }

    public int getCircuitIndex() {
        return circuitIndex;
    }

    public int getCircuitId() {
        return circuitId;
    }

    public int getTxSequenceNumber() {
        return txSequenceNumber;
    }

    public int getRxSequenceNumber() {
        return rxSequenceNumber;
    }

    public int getOpCodeAndFlags() {
        return opCodeAndFlags;
    }

    public boolean isChokeFlag() {
        return chokeFlag;
    }

    public boolean isNakFlag() {
        return nakFlag;
    }

    public boolean isMoreFollowsFlag() {
        return moreFollowsFlag;
    }

    public boolean isReserved() {
        return reserved;
    }

    public int getOpCode() {
        return opCode;
    }

    public AX25Callsign getOriginCallsign() {
        return originCallsign;
    }

    public AX25Callsign getDestinationCallsign() {
        return destinationCallsign;
    }

}
