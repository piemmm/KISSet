package org.prowl.kisset.protocols.netrom;

import org.prowl.ax25.AX25Callsign;
import org.prowl.kisset.protocols.core.Node;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.nio.ByteBuffer;
import java.text.ParseException;

/**
 * This will be a noteboard on netrom whilst I search for documentation.
 * Example packets seen:
 * <p>
 * AX25Frame[GB7MNK-1<GB7OUK,ctl=16,pid=cf,#=20]  body=8E 84 6E 9E AA 96 60 96 8A 8A A0 98 92 E0 01 00 00 00 00 05  <-- destination == KEEPLIp (p==0xE0 unconverted, so KEEPLI<0xE0>) - some form of BPQ keepalive?
 * <p>
 * session start: AX25Frame[GB7MNK-1<GB7OUK,ctl=3a,pid=cf,#=22]  body=8E 84 6E 9E AA 96 01 8E 84 6E 9A 9C 96 02 19 01 C4 00 90 02 04 19
 * AX25Frame[GB7MNK-1<GB7OUK,ctl=3c,pid=cf,#=165]  body=8E 84 6E 9E AA 96 60 8E 84 6E 9A 9C 96 02 19 01 C4 00 00 05 57 65 6C 63 6F 6D 65 20 74 6F 20 74 68 65 20 4F 6E 6C 69 6E 65 20 41 6D 61 74 65 75 72 20 52 61 64 69 6F 20 43 6F 6D 6D 75 6E 69 74 79 20 50 72 6F 74 6F 74 79 70 65 20 4E 6F 64 65 20 0D 4C 6F 63 61 6C 20 43 6F 6D 6D 61 6E 64 73 3E 20 43 48 41 54 20 43 4F 4E 4E 45 43 54 20 42 59 45 20 49 4E 46 4F 20 4E 45 57 53 20 4E 4F 44 45 53 20 52 4F 55 54 45 53 20 50 4F 52 54 53 20 54 45 4C 53 54 41 52 20 55 53 45 52 53 20 4D 48 45 41 52 44 0D
 * AX25Frame[GB7MNK-1<GB7OUK,ctl=7e,pid=cf,#=20]  body=8E 84 6E 9E AA 96 60 8E 84 6E 9A 9C 96 02 19 01 C4 00 00 03
 *
 * Notes:
 * Connect ack with choke or timeout will close.
 *
 */
public class NetROMPacket {

    //private static String body = "FF4D4E4B4E4F448E846E9A9C96644D4E4B4348548E846E9A9C9662FF8E846E9A9C96604D4E4B4242538E846E9A9C9662FF9A609C86AE40664352455343489A609C86AE4060BF8E846E9EAA96604F554B4E4F448E846E9EAA9660C08E846E9EAA96644F554B4348548E846E9EAA9660BF8E846E9EAA96664F554B4445568E846E9EAA9660BF9A846E9C98846042555A5A52449A846E9C988460C09A846E9C98846242555A4242539A846E9C988460969A846E9C98846442555A4348549A846E9C988460BF9A846E9C98846642555A5757439A846E9C988460BF";

    // List of opcodes
    public static final int OPCODE_PROTOCOL_EXTENSION = 0; // Protocol ID extension to network layer (IP type coded in circuit index, etc)
    public static final int OPCODE_CONNECT_REQUEST = 1; // Connect Request
    public static final int OPCODE_CONNECT_ACK = 2; // Connect Acknowledge
    public static final int OPCODE_DISCONNECT_REQUEST = 3; // Disconnect Request
    public static final int OPCODE_DISCONNECT_ACK = 4; // Disconnect Acknowledge
    public static final int OPCODE_INFORMATION_TRANSFER = 5; // Information Transfer
    public static final int OPCODE_INFORMATION_ACK = 6; // Information Acknowledge
    public static final int OPCODE_RESET = 7; // Reset
    public static final int OPCODE_EXTENDED_CONNECT_REQUEST = 8; // Extended Connection Request(xrouter?)

    // For opcode 0 (protocol extension) the circuit index holds the type of protocol - at the moment it's only 0x0C (IP)
    // If we're going to extend, then here will be the place to kick things off.
    public static final int PROTOCOL_IP = 0x0C; // IP protocol


    private final String originCallsign;
    private final String destinationCallsign;

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

    private final byte[] body;
    private final byte[] raw;

    /**
     * Decode a Net/ROM broadcast packet from an AX.25 frame
     **/
    public NetROMPacket(Node node) throws ParseException {
        this(node.getFrame().getBody());
    }

    public NetROMPacket(byte[] data) throws ParseException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        raw = data;

        // Src and dest callsigns
        originCallsign = PacketTools.getData(buffer, 7, true);
        destinationCallsign = PacketTools.getData(buffer, 7, true);
        ttl = buffer.get() & 0xFF;

        // Decode the Net/ROM header
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
        System.arraycopy(buffer.array(), buffer.position(), body, 0, buffer.remaining());
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

        if (isKeepAlivePacket()) {
            sb.append("NetROM Keep Alive Packet");
        } else {
            sb.append("NetROM Packet:");
        }

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
        String name;
        switch (opCode) {
            case 0:
                name = "Protocol ID extension to network layer"; // No documentation
                break;
            case 1:
                name = "Connect Request";
                break;
            case 2:
                name = "Connect Acknowledge";
                break;
            case 3:
                name = "Disconnect Request";
                break;
            case 4:
                name = "Disconnect Acknowledge";
                break;
            case 5:
                name = "Information Transfer";
                break;
            case 6:
                name = "Information Acknowledge";
                break;
            case 7:
                name = "Reset"; // Operation designed by G8PZT
                break;
            case 8:
                name = "Extended Connection Request";// CREQX - some undocumented xrouter thing
                break;
            default:
                name = "unknown";
        }

        sb.append("\r\n opCode=").append(opCode).append(" (").append(name).append(")");

        // If the opcode is 0, then we have a protocol extension, so we need to add more info.
        if (opCode == 0x0) {
            String extProtocol = "Unknown";
            if (circuitIndex == PROTOCOL_IP) {
                extProtocol = "IP";
            }
            sb.append("\r\n protocol extension=").append(circuitIndex).append(" ("+extProtocol+")");
        }


        sb.append("\r\n body:" + ANSI.NORMAL).append(Tools.byteArrayToReadableASCIIString(body));
        return sb.toString();
    }


    /**
     * For lack of documentation, this is apparently what a keepalive packet looks like.
     *
     * @return
     */
    public boolean isKeepAlivePacket() {

        try {
            if (circuitIndex == 0 && circuitId == 0 && txSequenceNumber == 0 && rxSequenceNumber == 0 && opCode == 5 && destinationCallsign.startsWith("KEEPLI") && raw[13] == (byte) 0xE0) {
                return true;
            }

        } catch (Throwable e) {
            // Ignore because it could be anything that doesn't fit.
        }

        return false;
    }


    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public void setCircuitIndex(int circuitIndex) {
        this.circuitIndex = circuitIndex;
    }

    public void setCircuitId(int circuitId) {
        this.circuitId = circuitId;
    }

    public void setTxSequenceNumber(int txSequenceNumber) {
        this.txSequenceNumber = txSequenceNumber;
    }

    public void setRxSequenceNumber(int rxSequenceNumber) {
        this.rxSequenceNumber = rxSequenceNumber;
    }

    public void setOpCodeAndFlags(int opCodeAndFlags) {
        this.opCodeAndFlags = opCodeAndFlags;
    }

    public void setChokeFlag(boolean chokeFlag) {
        this.chokeFlag = chokeFlag;
    }

    public void setNakFlag(boolean nakFlag) {
        this.nakFlag = nakFlag;
    }

    public void setMoreFollowsFlag(boolean moreFollowsFlag) {
        this.moreFollowsFlag = moreFollowsFlag;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public void setOpCode(int opCode) {
        this.opCode = opCode;
    }

    /**
     * Convert everything back to a packet
     *
     * @return
     */
    public byte[] toPacket() {

        ByteBuffer buffer = ByteBuffer.allocate(7 + 7 + 1 + 1 + 1 + 1 + 1 + body.length);
        buffer.put(new AX25Callsign(originCallsign).toByteArray());
        buffer.put(new AX25Callsign(destinationCallsign).toByteArray());
        buffer.put((byte) ttl);
        buffer.put((byte) circuitIndex);
        buffer.put((byte) circuitId);
        buffer.put((byte) txSequenceNumber);
        buffer.put((byte) rxSequenceNumber);

        // Now reconstruct the opcode and flags to send as well
        opCodeAndFlags = 0;
        if (chokeFlag) {
            opCodeAndFlags |= 0x80;
        }
        if (nakFlag) {
            opCodeAndFlags |= 0x40;
        }
        if (moreFollowsFlag) {
            opCodeAndFlags |= 0x20;
        }
        if (reserved) {
            opCodeAndFlags |= 0x10;
        }
        opCodeAndFlags |= opCode & 0x0F;
        buffer.put((byte) opCodeAndFlags);
        buffer.put(body);

        return buffer.array();
    }

    public byte[] getBody() {
        return body;
    }
}
