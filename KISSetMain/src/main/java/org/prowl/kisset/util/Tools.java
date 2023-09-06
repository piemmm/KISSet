package org.prowl.kisset.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.aprslib.parser.APRSPacket;
import org.prowl.aprslib.parser.APRSTypes;
import org.prowl.aprslib.parser.InformationField;
import org.prowl.aprslib.parser.Parser;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.protocols.core.Capability;
import org.prowl.kisset.protocols.core.Node;

import java.io.DataInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Tools {

    private static final Log LOG = LogFactory.getLog("Tools");


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

    /**
     * Make sure an IP address or hostname is valid
     * @return
     */
    public static boolean isValidIPorHostname(String ip) {
        if (ip == null) {
            return false;
        }
        if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return true;
        }
        if (ip.matches("[a-zA-Z0-9\\-\\.]+")) {
            return true;
        }
        return false;
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
     * Return an int from a string, or 'defaultValue' if the string is not an int
     * @param s
     * @return
     */
    public static int getInteger(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean isValidITUCallsign(String callsignToValidate) {
        return callsignToValidate.matches("\\A\\d?[a-zA-Z]{1,2}\\d{1,4}[a-zA-Z]{1,3}(-\\d+)?\\Z");
    }

    public static boolean isAlphaNumeric(String callsignToTest) {
        return callsignToTest.matches("[a-zA-Z0-9\\-]+");
    }


    /**
     * Determine the nodes capability from the packet types seen
     *
     * @param node
     * @param frame
     */
    public static void determineCapabilities(Node node, AX25Frame frame) {

        byte pid = frame.getPid();
        if (pid == AX25Frame.PID_NOLVL3) {
            // Check for APRS
            boolean isAprs = false;
            try {
                String aprsString = frame.sender.toString() + ">" + frame.dest.toString() + ":" + frame.getAsciiFrame();
                APRSPacket packet = Parser.parse(aprsString);
                if (packet.isAprs()) {
                    InformationField informationField = packet.getAprsInformation();
                    if (informationField != null) {
                        if (informationField.getAprsData(APRSTypes.T_POSITION) != null || informationField.getAprsData(APRSTypes.T_OBJECT) != null || informationField.getAprsData(APRSTypes.T_ITEM) != null) {
                            isAprs = true;
                        }
                    }
                }
            } catch (Throwable e) {
                // Ignore - probably not aprs. or unable to parse MICe
            }
            if (isAprs) {
                node.addCapabilityOrUpdate(new Capability(Node.Service.APRS));
            }

            // Check to see if this is a BBS - look for 'FBB' in the destination address starts with a message
            // fragment that looks like an FBB message broadcast
            // (because BPQ falsely broadcasts to FBB as well)
            if (frame.dest.toString().equals("FBB") && frame.getAsciiFrame().matches("[0-9][0-9]+ ")) {
                node.addCapabilityOrUpdate(new Capability(Node.Service.BBS));
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

        // Strip trailing newlines
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


    public static String byteArrayToReadableASCIIString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (b < 0x20 || b > 0xFA) {
                sb.append(ANSI.YELLOW + "<");
                sb.append(String.format("%02X", b));
                sb.append(">" + ANSI.NORMAL);
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    /**
     * Convenience method to read X amount of bytes from a stream
     *
     * @param din
     * @param length
     * @return
     */
    public static final String readString(DataInputStream din, int length) throws IOException {
        byte[] data = new byte[length];
        din.read(data, 0, length);
        return new String(data);
    }

    /**
     * Convenience method to read X amount of bytes from a stream
     *
     * @param din
     * @param length
     * @return
     */
    public static final byte[] readBytes(DataInputStream din, int length) throws IOException {
        byte[] data = new byte[length];
        din.read(data, 0, length);
        return data;
    }

    /**
     * Convenience method to return the sha-256 hash of a string
     *
     * @param s
     * @return
     */
    public static String hashString(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(s.getBytes());
            byte[] digest = md.digest();
            return byteArrayToHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * <p>
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     *
     * @returns Distance in Meters
     */
    public static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    /**
     * Convert a lat/lon to maidenhead locator
     * based on
     * https://ham.stackexchange.com/questions/221/how-can-one-convert-from-lat-long-to-grid-square
     */
    public static String toLocator(double lat, double lon) {

        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String locator = "";
        lon += 180;
        lat += 90;
        locator += alphabet.charAt((int) (lon / 20));
        locator += alphabet.charAt((int) (lat / 10));
        lon %= 20;
        lat %= 10;
        locator += (int) (lon / 2);
        locator += (int) lat;
        lon %= 2;
        lat %= 1;
        locator += alphabet.charAt((int) (lon / (5.0 / 60)));
        locator += alphabet.charAt((int) (lat / (2.5 / 60)));
        return locator;
    }


}
