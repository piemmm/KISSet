package org.prowl.kisset.util;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.prowl.aprslib.parser.APRSPacket;
import org.prowl.aprslib.parser.APRSTypes;
import org.prowl.aprslib.parser.Parser;
import org.prowl.kisset.ax25.AX25Frame;
import org.prowl.kisset.core.Capability;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.netrom.NetROMRoutingPacket;

import java.util.ArrayList;
import java.util.List;

public class Tools {

    public static final void runOnThread(Runnable runme) {
        Thread t = new Thread(runme);
        t.start();
    }

    /**
     * Used for things like rate limiting, avoiding spinning exception loops chewing CPU etc.
     *
     * @param millis
     */
    public static void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static String byteArrayToHexString(byte[] output) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < output.length; i++) {
            hexString.append(String.format("%02X", output[i]));
            hexString.append(" ");
        }
        return hexString.toString();
    }

    /**
     * Get a list of all the monospaced fonts available on the system.
     */
    public static List<String> getMonospacedFonts() {
        List<String> found = new ArrayList<>();
        final Text text1 = new Text("i i l i l i l i l");
        final Text text2 = new Text("A B C D E F G H I");
        for (String fontFamilyName : Font.getFamilies()) {
            // Get the font
            Font font = Font.font(fontFamilyName, FontWeight.NORMAL, FontPosture.REGULAR, 12d);
            text1.setFont(font);
            text2.setFont(font);
            if (text1.getLayoutBounds().getWidth() == text2.getLayoutBounds().getWidth()) {
                found.add(fontFamilyName);
            }
        }
        return found;
    }


    public static boolean isValidITUCallsign(String callsignToValidate) {
        return callsignToValidate.matches("\\A\\d?[a-zA-Z]{1,2}\\d{1,4}[a-zA-Z]{1,3}(-\\d+)?\\Z");
    }


    public static void determineCapabilities(Node node, AX25Frame frame) {

        byte pid = frame.getPid();
        if (pid == AX25Frame.PID_NOLVL3) {
            boolean isAprs = false;

            try {
                String aprsString = frame.sender.toString() + ">" + frame.dest.toString() + ":" + frame.getAsciiFrame();
                APRSPacket packet = Parser.parse(aprsString);
                isAprs = packet.getType() != APRSTypes.UNSPECIFIED;//packet.isAprs();
            } catch (Throwable e) {
                // Ignore - probably not aprs. or unable to parse MICe
            }
            if (isAprs) {
                node.addCapabilityOrUpdate(new Capability(Node.Service.APRS));
            }
        } else if (pid == AX25Frame.PID_NETROM) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.NETROM));
        } else if (pid == AX25Frame.PID_IP) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.IP));
        } else if (pid == AX25Frame.PID_VJC_TCPIP) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.VJ_IP));
        } else if (pid == AX25Frame.PID_FLEXNET) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.FLEXNET));
        } else if (pid == AX25Frame.PID_OPENTRAC) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.OPENTRAC));
        } else if (pid == AX25Frame.PID_TEXNET) {
            node.addCapabilityOrUpdate(new Capability(Node.Service.TEXNET));
        }

//        for (Capability c : node.getCapabilities()) {
//            LOG.debug("Node: " + node.getCallsign() + " supports " + c.getService().getName());
//        }

    }

    /**
     * Return a readable string of bytes from a supplied byte array - we strip everything that isn't a printable character.
     * CR and LF are allowed through as well as printable extended ASCII.
     *
     * @param data
     * @return A human readable string.
     */
    public static String readableTextOnlyFromByteArray(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (b >= 0x20 && b <= 0xFA) {
                sb.append((char) b);
            } else if (b == 0x0D || b == 0x0A) {
                sb.append("\r\n");

            }
        }
        return sb.toString();
    }


    public static String decodeNetROMToText(Node node) {

        NetROMRoutingPacket netROMRoutingPacket = new NetROMRoutingPacket(node);

        return netROMRoutingPacket.toString();


    }

}
