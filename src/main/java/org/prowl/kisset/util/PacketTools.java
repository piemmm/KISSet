package org.prowl.kisset.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.ax25.AX25Frame;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.netrom.NetROMRoutingPacket;
import org.prowl.kisset.netrom.inp3.L3RTTPacket;

import java.nio.ByteBuffer;

public class PacketTools {

    private static final Log LOG = LogFactory.getLog("PacketTools");
    public static final String CR = "\r\n";


    /**
     * Get a sequence of 6 or 7 bytes representing a callsign with optional bit shifting
     */
    public static String getData(ByteBuffer buffer, int length, boolean shift) {
        byte[] callsign = new byte[length];
        for (int i = 0; i < length; i++) {
            if (shift) {
                callsign[i] = (byte) ((buffer.get() & 0xFF) >> 1);
            } else {
                callsign[i] = buffer.get();
            }
        }

        return new String(callsign).trim();
    }

    /**
     * Get a sequence of 6 or 7 bytes representing a callsign with optional bit shifting
     */
    public static String getDataUntilSpaceOrCR(ByteBuffer buffer, int maxlength, boolean shift) {
        byte[] callsign = new byte[maxlength];
        int read = 0;
        for (int i = 0; i < maxlength; i++) {
            int b = buffer.get() & 0xFF;
            if (b == 0x20 || b == 0x0d) {
                break;
            }
            if (shift) {
                callsign[i] = (byte) (b >> 1);
            } else {
                callsign[i] = (byte) b;
            }
            read++;
        }

        return new String(callsign, 0, read);
    }


    /**
     * Decodes a packet with pid=0xcf which could be Net/ROM or inp3
     *
     * @param node
     * @return
     */
    public static String decodeNetROMToText(Node node) {

        try {
            if ((node.getFrame().getBody()[0] & 0xFF) == 0xFF) {
                NetROMRoutingPacket netROMRoutingPacket = new NetROMRoutingPacket(node);
                return netROMRoutingPacket.toString();
            } else {
                // Check to see if this is INP3/L3RTT
                L3RTTPacket l3rttPacket = new L3RTTPacket(node);
                return l3rttPacket.toString();
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }

        return "Unknown NetROM packet type:" + Tools.byteArrayToReadableASCIIString(node.getFrame().getBody());
    }

    public static String monitorPacketToString(HeardNodeEvent event) {
        Node node = event.getNode();
        StringBuilder builder = new StringBuilder();

        builder.append(CR);
        builder.append(ANSI.YELLOW);
        builder.append(node.getCallsign());
        builder.append(">");
        builder.append(node.getDestination());
        builder.append(ANSI.CYAN);
        builder.append("[");
        builder.append(node.getFrame().getFrameTypeString());
        if (node.getFrame().getP()) {
            builder.append(" P");
        }

        if (node.getFrame().getFrameType() == AX25Frame.FRAMETYPE_I || node.getFrame().getFrameType() == AX25Frame.FRAMETYPE_S) {
            builder.append(" NR=" + node.getFrame().getNR());
        }

        if (node.getFrame().getFrameType() == AX25Frame.FRAMETYPE_I) {
            builder.append(" NS=" + node.getFrame().getNS());
        }
        builder.append(" CTL=0x" + Integer.toString(node.getFrame().ctl & 0xFF, 16));
        String mod128 = "";
        if (node.getFrame().mod128) {
            builder.append(" CTL2=0x" + Integer.toString(node.getFrame().ctl & 0xFF, 16));
            mod128 = "MOD128";
        }
        builder.append(" PID=0x" + Integer.toString(node.getFrame().getPid() & 0xFF, 16));

        // Put the mod128 identifier in nice place
        if (mod128.length() > 0) {
            builder.append(" " + mod128);
        }
        builder.append("]");
        builder.append(ANSI.GREEN);
        builder.append(pidToString(node.getFrame().getPid()));
        builder.append(ANSI.NORMAL);
        if (node.getFrame().getByteFrame().length > 0) {
            builder.append(":");
            builder.append(CR);
            if (node.getFrame().getPid() == AX25Frame.PID_NETROM) {
                // NetRom frame
                builder.append(ANSI.MAGENTA + PacketTools.decodeNetROMToText(node) + ANSI.NORMAL);
            } else {
                // Normal I (or unknown payload) frame
                builder.append(Tools.readableTextOnlyFromByteArray(node.getFrame().getByteFrame()));
            }
        }
        builder.append(CR);
        return builder.toString();
    }


    public static String pidToString(byte pid) {

        StringBuilder pidString = new StringBuilder();
        switch (pid) {
            case AX25Frame.PID_X25_PLP:
                pidString.append("{CCITT X.25 PLP/ROSE}");
                break;
            case AX25Frame.PID_VJC_TCPIP:
                pidString.append("{Compressed TCP/IP packet}");
                break;
            case AX25Frame.PID_VJUC_TCPIP:
                pidString.append("{Uncompressed TCP/IP packet}");
                break;
            case AX25Frame.PID_SEG_FRAG:
                pidString.append("{Segmentation fragment}");
                break;
//            case AX25Frame.PID_UNSEGMENT:
//                pidString.append("{Unsegmented frame}");
//                break;
            case AX25Frame.PID_TEXNET:
                pidString.append("{TEXNET datagram protocol}");
                break;
            case AX25Frame.PID_LQP:
                pidString.append("{Link Quality Protocol}");
                break;
            case AX25Frame.PID_ATALK:
                pidString.append("{Appletalk}");
                break;
            case AX25Frame.PID_AARP:
                pidString.append("{Appletalk ARP}");
                break;
            case AX25Frame.PID_IP:
                pidString.append("{ARPA Internet Protocol}");
                break;
            case AX25Frame.PID_IARP:
                pidString.append("{ARPA Internet ARP}");
                break;
            case AX25Frame.PID_FLEXNET:
                pidString.append("{FlexNet}");
                break;
            case AX25Frame.PID_NETROM:
                pidString.append("{Net/ROM}");
                break;
            case AX25Frame.PID_NOLVL3:
                pidString.append("{No LVL3}");
                break;
            case AX25Frame.PID_ESCAPE:
                pidString.append("{Reserved for AX.25 (escape character)}");
                break;
            default:
                pidString.append("{Unknown}");
                break;
        }


        return pidString.toString();
    }

}
