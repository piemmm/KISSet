package org.prowl.kisset.netrom;

import org.prowl.kisset.ax25.AX25Callsign;
import org.prowl.kisset.ax25.AX25Frame;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.nio.ByteBuffer;
import java.util.StringTokenizer;

/**
 * Example packets seen:
 *
 * AX25Frame[GB7MNK-1<GB7OUK,ctl=16,pid=cf,#=20]  body=8E 84 6E 9E AA 96 60 96 8A 8A A0 98 92 E0 01 00 00 00 00 05
 *
 * session start: AX25Frame[GB7MNK-1<GB7OUK,ctl=3a,pid=cf,#=22]  body=8E 84 6E 9E AA 96 01 8E 84 6E 9A 9C 96 02 19 01 C4 00 90 02 04 19
 * AX25Frame[GB7MNK-1<GB7OUK,ctl=3c,pid=cf,#=165]  body=8E 84 6E 9E AA 96 60 8E 84 6E 9A 9C 96 02 19 01 C4 00 00 05 57 65 6C 63 6F 6D 65 20 74 6F 20 74 68 65 20 4F 6E 6C 69 6E 65 20 41 6D 61 74 65 75 72 20 52 61 64 69 6F 20 43 6F 6D 6D 75 6E 69 74 79 20 50 72 6F 74 6F 74 79 70 65 20 4E 6F 64 65 20 0D 4C 6F 63 61 6C 20 43 6F 6D 6D 61 6E 64 73 3E 20 43 48 41 54 20 43 4F 4E 4E 45 43 54 20 42 59 45 20 49 4E 46 4F 20 4E 45 57 53 20 4E 4F 44 45 53 20 52 4F 55 54 45 53 20 50 4F 52 54 53 20 54 45 4C 53 54 41 52 20 55 53 45 52 53 20 4D 48 45 41 52 44 0D
 *AX25Frame[GB7MNK-1<GB7OUK,ctl=7e,pid=cf,#=20]  body=8E 84 6E 9E AA 96 60 8E 84 6E 9A 9C 96 02 19 01 C4 00 00 03
 */
public class NetROMPacket {

    //private static String body = "FF4D4E4B4E4F448E846E9A9C96644D4E4B4348548E846E9A9C9662FF8E846E9A9C96604D4E4B4242538E846E9A9C9662FF9A609C86AE40664352455343489A609C86AE4060BF8E846E9EAA96604F554B4E4F448E846E9EAA9660C08E846E9EAA96644F554B4348548E846E9EAA9660BF8E846E9EAA96664F554B4445568E846E9EAA9660BF9A846E9C98846042555A5A52449A846E9C988460C09A846E9C98846242555A4242539A846E9C988460969A846E9C98846442555A4348549A846E9C988460BF9A846E9C98846642555A5757439A846E9C988460BF";

    private String originCallsign;
    private String destinationCallsign;

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

    private byte[] body;

    /**
     * Decode a Net/ROM broadcast packet from an AX.25 frame
     **/
    public NetROMPacket(Node node) {
        ByteBuffer buffer = ByteBuffer.wrap(node.getFrame().getBody());
        // Src and dest callsigns
        originCallsign = PacketTools.getData(buffer, 7, true);
        destinationCallsign = PacketTools.getData(buffer, 7, true);
        ttl = buffer.get() & 0xFF;

        // Decode the Nect/ROM header
        circuitIndex = buffer.get() & 0xFF;
        circuitId = buffer.get() & 0xFF;
        txSequenceNumber = buffer.get() & 0xFF;
        rxSequenceNumber = buffer.get() & 0xFF;
        opCodeAndFlags = buffer.get() & 0xFF;

        // Now decode the opcode and flags
        chokeFlag = (opCodeAndFlags & 0x80) != 0;
        nakFlag = (opCodeAndFlags & 0x40) != 0;
        moreFollowsFlag = (opCodeAndFlags & 0x20) != 0;
        reserved = (opCodeAndFlags & 0x10) != 0;
        opCode = opCodeAndFlags & 0x0F;

        body = new byte[buffer.remaining()];
        System.arraycopy(buffer.array(), buffer.position(), body,0, buffer.remaining());


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

    public String getOriginCallsign() {
        return originCallsign;
    }

    public String getDestinationCallsign() {
        return destinationCallsign;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NetROMPacket:");
        sb.append("\r\n originCallsign=").append(originCallsign);
        sb.append("\r\n destinationCallsign=").append(destinationCallsign);
        sb.append("\r\n ttl=").append(ttl);
        sb.append("\r\n circuitIndex=").append(circuitIndex);
        sb.append("\r\n circuitId=").append(circuitId);
        sb.append("\r\n txSequenceNumber=").append(txSequenceNumber);
        sb.append("\r\n rxSequenceNumber=").append(rxSequenceNumber);
        sb.append("\r\n opCodeAndFlags=0x").append(Integer.toString(opCodeAndFlags, 16));
        sb.append("\r\n chokeFlag=").append(chokeFlag);
        sb.append("\r\n nakFlag=").append(nakFlag);
        sb.append("\r\n moreFollowsFlag=").append(moreFollowsFlag);
        sb.append("\r\n reserved=").append(reserved);
        sb.append("\r\n opCode=").append(opCode);
        sb.append("\r\n body:"+ ANSI.NORMAL).append(Tools.byteArrayToReadableASCIIString(body));
        return sb.toString();
    }

}
