package org.prowl.ax25.util;

public class AX25Tools {


    /**
     * Convert a byte array to a hex string
     *
     * @param output
     * @return a String
     */
    public static String byteArrayToHexString(byte[] output) {
        if (output == null) {
            return "null array";
        }
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < output.length; i++) {
            hexString.append(String.format("%02X", output[i]));
            hexString.append(" ");
        }
        return hexString.toString();
    }

    public static String byteArrayToReadableASCIIString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (b < 0x20 || b > 0xFA) {
                sb.append("<");
                sb.append(String.format("%02X", b));
                sb.append(">");
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

}
