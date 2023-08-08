package org.prowl.kisset.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.netrom.NetROMRoutingPacket;
import org.prowl.kisset.netrom.inp3.L3RTTPacket;

import java.nio.ByteBuffer;

public class PacketTools {

    private static final Log LOG = LogFactory.getLog("PacketTools");


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
                callsign[i] = (byte)b;
            }
            read++;
        }

        return new String(callsign,0,read);
    }


    /**
     * Decodes a packet with pid=0xcf which could be Net/ROM or inp3
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
        } catch(Throwable e) {
            LOG.error(e.getMessage(), e);
        }

        return "Unknown NetROM packet type:"+Tools.byteArrayToReadableASCIIString(node.getFrame().getBody());
    }



}
