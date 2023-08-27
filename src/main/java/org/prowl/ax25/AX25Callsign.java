package org.prowl.ax25;
/*
 * Copyright (C) 2011-2022 Andrew Pavlin, KA2DDO
 * This file is part of YAAC (Yet Another APRS Client).
 *
 *  YAAC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  YAAC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and GNU Lesser General Public License along with YAAC.  If not,
 *  see <http://www.gnu.org/licenses/>.
 */


import org.prowl.ax25.util.StringCache;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;

/**
 * This class defines a single AX.25 callsign (address).
 *
 * @author Andrew Pavlin, KA2DDO
 */
public final class AX25Callsign implements Comparable<AX25Callsign>, Cloneable, Serializable {
    /**
     * A Comparator to use for callsigns when it is more efficient than using the
     * Comparable interface to AX25Callsign.
     *
     * @see #compareTo(AX25Callsign)
     */
    public static final Comparator<AX25Callsign> CALLSIGN_COMPARATOR = new Comparator<AX25Callsign>() {
        public int compare(AX25Callsign o1, AX25Callsign o2) {
            int diff = o1.callsign.compareToIgnoreCase(o2.callsign);
            if (0 == diff && o1.valid == o2.valid) {
                diff = ((int) o1.ssid & 0xFF) - ((int) o2.ssid & 0xFF);
            }
            return diff;
        }
    };
    private static final long serialVersionUID = -1967268147133231444L;
    /**
     * Default value for AX.25 reserved bits (initialized to protocol default value).
     */
    private static byte defaultReserved = 3;
    /**
     * Has_been_repeated flag (for digipeater callsigns) or command/response flags (for destination and source callsigns).
     */
    public boolean h_c;
    /**
     * Flag bit in SSID byte indicating this is the last callsign in a digipeater sequence.
     */
    public boolean last;
    /**
     * Indicates whether the callsign in this object can be exported as a valid AX.25 binary protocol address.
     */
    public boolean valid = true;
    /**
     * AX.25 reserved bits (initialized to protocol default value).
     */
    byte reserved = defaultReserved;
    private String callsign;
    private byte ssid;
    private transient String cachedToString;

    /**
     * Construct an empty but assumed-valid callsign.
     */
    public AX25Callsign() {
    }

    /**
     * Construct a AX25Callsign from the string representation of the callsign.
     *
     * @param textCallsign String to parse into an AX.25-compliant callsign
     */
    public AX25Callsign(String textCallsign) {
        textCallsign = textCallsign.trim();
        int pos;
        int length;
        if ((pos = textCallsign.indexOf('-')) < 0) {
            if ((length = textCallsign.length()) < 2) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too short");
                valid = false;
            } else if (length > 6) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too long");
                valid = false;
            } else {
                valid = validateCallsign(textCallsign, 0, length);
            }
            ssid = 0;
            callsign = textCallsign; // don't cache in this case, too likely to be a MicE destination
            cachedToString = textCallsign;
        } else {
            if (pos < 2) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too short");
                valid = false;
            } else if (pos > 6) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too long");
                valid = false;
            } else {
                valid = validateCallsign(textCallsign, 0, pos);
            }
            if (pos + 1 < (length = textCallsign.length())) {
                for (int i = pos + 1; i < length; i++) {
                    char ch;
                    if ((ch = textCallsign.charAt(i)) < '0' || ch > '9') {
                        valid = false;
                        break;
                    }
                }
            } else {
                // we have the hyphen, but no trailing SSID number
                valid = false;
            }
            if (valid) {
                int tmpSsid;
                if ((tmpSsid = Integer.parseInt(textCallsign.substring(pos + 1))) < 0 || tmpSsid > 15) {
//                    throw new IllegalArgumentException("out-of-range SSID=" + tmpSsid);
                    valid = false;
                }
                ssid = (byte) tmpSsid;
                callsign = StringCache.intern(textCallsign.substring(0, pos));
            } else {
                callsign = StringCache.intern(textCallsign);
            }
        }
    }

    /**
     * Construct a AX25Callsign from the specified sub-string representation of the callsign.
     *
     * @param textCallsign String to parse into an AX.25-compliant callsign
     * @param startPos     int position in string where callsign starts
     * @param endPos       int position in string where callsign ends
     */
    public AX25Callsign(String textCallsign, int startPos, int endPos) {
        int pos = textCallsign.indexOf('-', startPos);
        if (-1 == pos || pos >= endPos) {
            pos = endPos;
            if (endPos - startPos < 2) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too short");
                valid = false;
            } else if (endPos - startPos > 6) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too long");
                valid = false;
            } else {
                valid = validateCallsign(textCallsign, startPos, pos);
            }
            ssid = 0;
            callsign = StringCache.intern(textCallsign.substring(startPos, endPos));
        } else {
            if (pos - startPos < 2) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too short");
                valid = false;
            } else if (pos - startPos > 6) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too long");
                valid = false;
            } else {
                valid = validateCallsign(textCallsign, startPos, pos);
            }
            for (int i = pos + 1; i < endPos; i++) {
                char ch = textCallsign.charAt(i);
                if (ch < '0' || ch > '9') {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                String ssidString = textCallsign.substring(pos + 1, endPos);
                int tmpSsid = Integer.parseInt(ssidString);
                if (tmpSsid < 0 || tmpSsid > 15) {
//                    throw new IllegalArgumentException("out-of-range SSID=" + tmpSsid);
                    valid = false;
                }
                ssid = (byte) tmpSsid;
                callsign = StringCache.intern(textCallsign.substring(startPos, pos));
            } else {
                callsign = StringCache.intern(textCallsign.substring(startPos, endPos));
            }
        }
    }

    /**
     * Construct a AX25Callsign from the specified part of the byte array of text containing the callsign.
     *
     * @param startPos     int position in byte array where callsign starts
     * @param endPos       int position in byte array where callsign ends
     * @param byteCallsign byte array of ASCII text to parse into an AX.25-compliant callsign
     */
    public AX25Callsign(int startPos, int endPos, byte[] byteCallsign) {
        int pos = AX25Message.indexOf(byteCallsign, endPos, '-', startPos);
        if (-1 == pos || pos >= endPos) {
            pos = endPos;
            ssid = 0;
            callsign = new String(byteCallsign, 0, startPos, pos); // must be ASCII
            if (endPos - startPos < 2) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too short");
                valid = false;
            } else if (endPos - startPos > 6) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too long");
                valid = false;
            } else {
                valid = validateCallsign(callsign, 0, endPos - startPos);
            }
        } else {
            if (pos - startPos < 2) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too short");
                valid = false;
            } else if (pos - startPos > 6) {
//                    throw new IllegalArgumentException("callsign " + textCallsign + " too long");
                valid = false;
            }
            for (int i = pos + 1; i < endPos; i++) {
                char ch = (char) byteCallsign[i];
                if (ch < '0' || ch > '9') {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                int tmpSsid = Integer.parseInt(new String(byteCallsign, 0, pos + 1, endPos - pos - 1));
                if (tmpSsid < 0 || tmpSsid > 15) {
//                    throw new IllegalArgumentException("out-of-range SSID=" + tmpSsid);
                    valid = false;
                }
                ssid = (byte) tmpSsid;
                callsign = new String(byteCallsign, 0, startPos, pos); // must be ASCII
            } else {
                callsign = new String(byteCallsign, 0, startPos, endPos); // must be ASCII
            }
        }
    }

    /**
     * Extract an AX.25 callsign from an AX.25 frame byte array in network byte order. This
     * variant properly handles all the control bits and the fact that the ASCII characters are
     * shifted left one bit according to the AX.25 protocol specification.
     *
     * @param buf    byte array containing the AX.25-encoded callsign
     * @param offset index into the array where the callsign begins
     * @param length bytes remaining in the array after the offset
     * @throws IndexOutOfBoundsException if not enough bytes left in the array to contain an AX.25 callsign
     * @throws IllegalArgumentException  if the callsign has an invalid format, such as embedded whitespace in
     *                                   the middle of a callsign with following non-blank characters
     */
    public AX25Callsign(byte[] buf, int offset, int length) throws IndexOutOfBoundsException, IllegalArgumentException {
        if (length < 7 || buf.length - offset < length) {
            throw new IndexOutOfBoundsException("not enough data left for callsign: len=" + length+"  buflen="+buf.length+"  off="+offset);
        }
        char[] b = new char[6];
        int len = 0;
        boolean inPadSpaces = false;
        for (int i = 0; i < 6; i++) {
            char ch = (char) (((int) buf[offset + i] & 0xFF) >> 1);
            if (' ' == ch) {
                inPadSpaces = true;
            } else if (!inPadSpaces) {
//                if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                b[len++] = ch;
/*
                } else {
                    throw new IllegalArgumentException("character '" + ch + "' (0x" + Integer.toString((int)ch & 0xFFFF, 16) + ") not valid in a callsign");
                }
*/
            } else {
                throw new IllegalArgumentException("no characters allowed after whitespace in callsign '" + new String(buf, 0, offset, 6) + "'");
            }
        }
        callsign = StringCache.intern(new String(b, 0, len));
        int b6 = buf[offset + 6];
        h_c = (b6 & 0x80) != 0;
        reserved = (byte) ((b6 & 0x60) >> 5);
        ssid = (byte) ((b6 & 0x1E) >> 1);
        last = (b6 & 0x01) != 0;
    }

    private static boolean validateCallsign(String callsign, int startPos, int endPos) {
        for (int pos = startPos; pos < endPos; pos++) {
            char ch = callsign.charAt(pos);
            if (!(ch >= '0' && ch <= '9') && !(ch >= 'A' && ch <= 'Z') && !(ch >= 'a' && ch <= 'z')) {
//                throw new IllegalArgumentException("invalid characters in callsign '" + callsign.substring(0, length) + "'");
                return false;
            }
        }
        return !callsign.startsWith("NOCALL", startPos);
    }

    /**
     * Test if a callsign looks like a real callsign (at least one digit somewhere other than
     * the last character, all letters uppercase). Note this will automatically strip off the SSID
     * (if any) before testing. Note this is safe for empty strings, and will properly report them
     * as not being a valid real-station callsign,
     *
     * @param callsign String callsign to test
     * @return boolean true if callsign looks like real
     */
    public static boolean isRealCallsign(String callsign) {
        boolean hasDigit = false;
        boolean hasLetter = false;
        boolean allUppercase = true;
        int lastCharPos = callsign.length() - 1;
        int hyphenPos = callsign.lastIndexOf('-', lastCharPos);
        if (hyphenPos > 0 && hyphenPos < lastCharPos) {
            lastCharPos = hyphenPos - 1;
        } else /*if (-1 == hyphenPos)*/ {
            hyphenPos = callsign.lastIndexOf(' ', lastCharPos); // handle possible Dstar callsigns in object names
            if (hyphenPos > 0 && hyphenPos < lastCharPos) {
                lastCharPos = hyphenPos - 1;
                while (lastCharPos > 0 && callsign.charAt(lastCharPos) == ' ') {
                    lastCharPos--;
                }
            }
        }
        if (lastCharPos >= 6 || lastCharPos <= 2) {
            return false; // too long or short to be real
        }
        for (int i = lastCharPos; i >= 0; i--) {
            char ch = callsign.charAt(i);
            if (i < lastCharPos && ch >= '0' && ch <= '9') {
                hasDigit = true;
            } else if (ch >= 'A' && ch <= 'Z') {
                hasLetter = true;
            } else if (ch >= 'a' && ch <= 'z') {
                allUppercase = false;
            } else {
                return false; // not a legal character for a callsign
            }
        }
        return hasDigit && hasLetter && allUppercase;
    }

    /**
     * Test if a callsign looks like a real callsign (at least one digit somewhere other than
     * the last character, all letters uppercase), and an AX.25-legal SSID.
     * Note this is safe for empty strings, and will properly report them
     * as not being a valid real-station callsign,
     *
     * @param callsign String callsign to test
     * @return boolean true if callsign looks like real
     */
    public static boolean isDigipeatableCallsign(String callsign) {
        boolean hasDigit = false;
        boolean hasLetter = false;
        boolean allUppercase = true;
        int lastCharPos = callsign.length() - 1;
        int ssid = 0;
        int hyphenPos = callsign.lastIndexOf('-', lastCharPos);
        if (hyphenPos > 0 && hyphenPos < lastCharPos) {
            lastCharPos = hyphenPos - 1;
            try {
                ssid = Integer.parseInt(callsign.substring(hyphenPos + 1));
            } catch (NumberFormatException e) {
                return false; // if a SSID can't be decoded, it's not legal for AX.25 digipeaters
            }
            if (ssid < 0 || ssid > 15) {
                return false;
            }
        }
        if (lastCharPos >= 6 || lastCharPos <= 2) {
            return false; // too long or short to be real
        }
        for (int i = lastCharPos; i >= 0; i--) {
            char ch = callsign.charAt(i);
            if (i < lastCharPos && ch >= '0' && ch <= '9') {
                hasDigit = true;
            } else if (ch >= 'A' && ch <= 'Z') {
                hasLetter = true;
            } else if (ch >= 'a' && ch <= 'z') {
                allUppercase = false;
            } else {
                return false; // not a legal character for a callsign
            }
        }
        return hasDigit && hasLetter && allUppercase;
    }

    /**
     * Test if the parameter appears to be a valid New n-N digipeat alias. Such callsigns are all
     * uppercase ASCII letters except for the last character, which must be a digit between 1 and 7,
     * and the SSID (if present) must be less than or equal to the number corresponding to that digit (i.e.,
     * WIDE1-7 is not valid).
     *
     * @param callsign String supposedly containing an AX.25 callsign
     * @return boolean true if this callsign looks like a New n-N digipeat alias
     */
    public static boolean isNewNParadigmAlias(String callsign) {
        char ch;
        int lastCharPos = callsign.length() - 1;
        int ssid = 0;
        int hyphenPos = callsign.lastIndexOf('-', lastCharPos);
        if (hyphenPos > 0 && hyphenPos < lastCharPos) {
            lastCharPos = hyphenPos - 1;
            try {
                ssid = Integer.parseInt(callsign.substring(hyphenPos + 1));
            } catch (NumberFormatException e) {
                // ignore
            }
        } else /*if (-1 == hyphenPos)*/ {
            if ((hyphenPos = callsign.lastIndexOf(' ', lastCharPos)) > 0 &&
                    hyphenPos < lastCharPos) {
                return false; // D-star gateway names can't be digipeat aliases
            }
        }
        if (lastCharPos >= 6 || lastCharPos < 2) {
            return false; // too long or short to be legitimate
        }
        ch = callsign.charAt(lastCharPos);
        if (ch >= '1' && ch <= '7' && ssid <= (ch - '0')) {
            for (int i = lastCharPos - 1; i >= 0; i--) {
                ch = callsign.charAt(i);
                if (ch < 'A' || ch > 'Z') {
                    return false; // not a legal character for a New-N alias
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Test if the parameter appears to be an old paradign digipeat alias. Such callsigns are all
     * uppercase ASCII letters.
     *
     * @param callsign String supposedly containing an AX.25 callsign
     * @return boolean true if this callsign looks like an old digipeat alias
     */
    public static boolean isOldParadigmAlias(String callsign) {
        return "WIDE".equals(callsign) || "RELAY".equals(callsign) || "TRACE".equals(callsign);
    }

    /**
     * Get the current default value for the reserved bits of the AX25 callsign SSID byte.
     *
     * @return current default RR bit value
     */
    public static byte getDefaultReserved() {
        return defaultReserved;
    }

    /**
     * Set the default value for the reserved bits of newly generated AX25 callsign SSID byte.
     *
     * @param defaultReserved current default RR bit value
     */
    public static void setDefaultReserved(byte defaultReserved) {
        AX25Callsign.defaultReserved = (byte) (defaultReserved & 3);
    }

    /**
     * Test if this callsign appears to be a valid New n-N digipeat alias. Such callsigns are all
     * uppercase ASCII letters except for the last character, which must be a digit between 1 and 7,
     * and the SSID must be less than or equal to the number corresponding to that digit (i.e.,
     * WIDE1-7 is not valid).
     *
     * @return boolean true if this callsign looks like a New n-N digipeat alias
     */
    public boolean isNewNParadigmAlias() {
        if (valid) {
            int len = callsign.length();
            if (len >= 2) {
                char ch;
                ch = callsign.charAt(len - 1);
                if (ch >= '1' && ch <= '7') {
                    if (ssid <= ch - '0') {
                        for (int i = len - 2; i >= 0; i--) {
                            ch = callsign.charAt(i);
                            if (ch < 'A' || ch > 'Z') {
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Test if some other object is the same as this AX25Callsign.
     *
     * @param o Object to compare against this callsign
     * @return boolean true if o is a AX25Callsign with the same value
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof AX25Callsign that)) return false;

        return (ssid == that.ssid) &&
                callsign.equals(that.callsign);
    }

    /**
     * Returns a hash code for this callsign.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = callsign.hashCode();
        return (hash << 5) + hash + ssid;
    }

    /**
     * Return a String representing this AX25Callsign object.
     *
     * @return descriptive String
     */
    @Override
    public String toString() {
        if (ssid == 0) {
            return callsign;
        } else {
            String s;
            if ((s = cachedToString) != null) {
                return s;
            } else {
                if (isRealCallsign(callsign)) {
                    return cachedToString = StringCache.intern(callsign + '-' + ssid);
                } else {
                    // if it looks like a Mic-E destination, don't clutter the cache
                    return cachedToString = callsign + '-' + ssid;
                }
            }
        }
    }

    /**
     * Return a String representing this AX25Frame object, with an additional note if the reserved field in the SSID byte
     * is not the default value.
     *
     * @return descriptive String
     */
    public String toAnnotatedString() {
        if (3 != reserved) {
            return toString() + '[' + Integer.toBinaryString(reserved) + ']';
        }
        return toString();
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object. Callsigns are
     * compared in the ASCII order of their strings, then using their SSIDs
     * as a tie-breaker. Note that lowercase is not supposed to be used in
     * a callsign, but this comparator ignores it.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this object.
     */
    public int compareTo(AX25Callsign o) {
        int diff = String.CASE_INSENSITIVE_ORDER.compare(callsign, o.callsign);
        if (0 == diff && valid == o.valid) {
            diff = ((int) ssid & 0xFF) - ((int) o.ssid & 0xFF);
/*
            if (0 == diff) {
                // already-repeated callsigns go first
                diff = (h_c ? 0 : 1) - (o.h_c ? 0 : 1);
            }
*/
        }
        return diff;
    }

    /**
     * Encode this AX25Callsign into binary radio transmission format on a stream.
     *
     * @param os   the OutputStream to write the binary encoding to
     * @param last boolean true if this callsign should have the last bit set in
     *             its last byte to indicate there will be no following callsigns
     *             according to the AX.25 protocol specification
     * @throws IOException if callsign could not be written to the stream
     */
    public void write(OutputStream os, boolean last) throws IOException {
/*
        if (!valid) {
            throw new IllegalArgumentException("can't transmit this callsign \"" + callsign + "\", not AX.25 compliant format");
        }
*/
        int i = 0;
        for (; i < Math.min(callsign.length(), 6); i++) {
            os.write(((int) callsign.charAt(i) & 0x7F) << 1);
        }
        for (; i < 6; i++) {
            os.write(0x40); // space shifted left 1
        }
        os.write((ssid << 1) | ((reserved & 3) << 5) | (h_c ? 0x80 : 0) | (last ? 1 : 0));
    }

    /**
     * Return the callsign as the actual byte sequence that would be transmitted
     * over the air (without HDLC bit-stuffing).
     *
     * @return byte array of the callsign
     */
    public byte[] toByteArray() {
        byte[] answer = new byte[7];
        int i = 0;
        for (; i < Math.min(callsign.length(), 6); i++) {
            answer[i] = (byte) (((int) callsign.charAt(i) & 0x7F) << 1);
        }
        for (; i < 6; i++) {
            answer[i] = (0x40); // space shifted left 1
        }
        answer[6] = (byte) ((ssid << 1) | ((reserved & 3) << 5) | (h_c ? 0x80 : 0) | (last ? 1 : 0));
        return answer;
    }

    /**
     * Return the AX.25 packing of the 7th byte of the callsign. Used to support the optimized
     * checksum computation in AX25Frame so as to avoid malloc'ing a byte array by using the
     * toByteArray() method.
     *
     * @return 8-bit value for encoded 7th byte of wire-format AX.25 callsign
     * @see AX25Frame#getChecksum()
     * @see #toByteArray()
     */
    int get7thByte() {
        return ((ssid << 1) | ((reserved & 3) << 5) | (h_c ? 0x80 : 0) | (last ? 1 : 0));
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a clone of this instance.
     */
    @Override
    public Object clone() {
        return dup();
    }

    /**
     * Create a shallow clone of this AX25Callsign, discarding any cached toString() value.
     *
     * @return a copy AX25Callsign
     */
    public AX25Callsign dup() {
        AX25Callsign c = new AX25Callsign();
        c.callsign = callsign;
        c.h_c = h_c;
        c.reserved = reserved;
        c.ssid = ssid;
        c.last = last;
        c.valid = valid;
        return c;
    }

    /**
     * Gets the base callsign (without the AX.25 SSID extension). Note that the extension may
     * be included in this string if the overall callsign does not comply with AX.25 requirements.
     *
     * @return String of the base callsign name
     */
    public String getBaseCallsign() {
        return callsign;
    }

    /**
     * Return the numeric SSID associated with this callsign. Will be zero if the overall callsign
     * does not comply with AX.25 requirements.
     *
     * @return numeric SSID in the range 0 to 15
     */
    public int getSSID() {
        return ssid;
    }

    /**
     * Specify the numeric SSID associated with this callsign.
     *
     * @param ssid numeric SSID in the range 0 to 15
     */
    public void setSSID(int ssid) {
        if (this.ssid != ssid) {
            if (ssid < 0 || ssid > 15) {
                throw new IllegalArgumentException("out-of-range SSID value=" + ssid);
            }
            this.ssid = (byte) ssid;
            cachedToString = null;
        }
    }

    /**
     * Decrement a non-zero SSID value, as is done for NewN-n paradigm digipeat aliases.
     */
    public void decrementSSID() {
        if (ssid > 0) {
            ssid--;
            cachedToString = null;
        }
    }

    /**
     * Report if the hasBeenRepeated flag in the callsign is set.
     *
     * @return boolean true if the hasBeenRepeated bit is set
     */
    public boolean hasBeenRepeated() {
        return h_c;
    }

    /**
     * Get the reserved bits of the 7th byte of the callsign (per the AX.25 specification).
     *
     * @return the value of the reserved bits
     */
    public byte getReserved() {
        return reserved;
    }

    /**
     * Set the reserved bits of the 7th byte of the callsign (per the AX.25 specification).
     *
     * @param reserved the new value of the reserved bits (masked to the range 0 to 3)
     */
    public void setReserved(byte reserved) {
        this.reserved = (byte) (reserved & 3);
    }

    /**
     * Indicates if this callsign has valid syntax to be transmitted in the header of an AX.25 frame.
     *
     * @return boolean true if callsign is legal for AX.25 frame transmission
     */
    public boolean isValid() {
        return valid;
    }
}
