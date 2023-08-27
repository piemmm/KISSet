package org.prowl.ax25;
/*
 * Copyright (C) 2011-2023 Andrew Pavlin, KA2DDO
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

import java.io.Serializable;
import java.util.*;

/**
 * This class defines the common infrastructure for one decoded AX.25 message. Subclasses implement
 * the details for different layer 3 and above protocols.
 *
 * @author Andrew Pavlin, KA2DDO
 */
abstract public class AX25Message implements Comparable<AX25Message>, Serializable, Cloneable {
    /**
     * Reserved constant for non-expiring objects' timestamp.
     */
    public static final long PERMANENT = -1L;
    /**
     * A read-only protocol set that includes both APRS and OpenTRAC.
     *
     * @see #getProtocols()
     */
    protected static final Set<ProtocolFamily> APRS_AND_OPENTRAC = Collections.unmodifiableSet(EnumSet.of(ProtocolFamily.APRS, ProtocolFamily.OPENTRAC));
    static final Set<ProtocolFamily> RAW_AX25_ONLY = Collections.singleton(ProtocolFamily.RAW_AX25);
    /**
     * The AX.25 frame object from which this Message was extracted.
     */
    public AX25Frame ax25Frame;
    /**
     * Callsign of the station originating this message. This may not match the sender callsign
     * if the message was relayed through a third-party network (such as APRS-IS). In such cases,
     * sender will be the callsign of the station transmitting the message onto RF, and originatingCallsign
     * will be taken from the third-party routing information to indicate the station that
     * originally injected this message into the network of networks.
     *
     * @see AX25Frame#sender
     */
    public String originatingCallsign;
    /**
     * Destination "callsign" from the station originating this message. This may not match the destination callsign
     * if the message was relayed through a third-party network (such as APRS-IS). In such cases,
     * destination will be the "tocall" (assuming APRS) of the station transmitting the message onto RF, and originatingDest
     * will be taken from the third-party routing information to indicate the destination that
     * originally injected this message into the network of networks.
     *
     * @see AX25Frame#dest
     */
    public String originatingDest;
    /**
     * The entire third-party routing path for this AX25Message, or null if this AX25message
     * is still on its original network.
     */
    public String thirdParty = null;
    /**
     * Message timestamp in Java standard milliseconds since 1970 UTC. May be different from rcptTime if
     * the message body has a time value in it.
     */
    public long timestamp;
    /**
     * The time the message was received by the system in Java standard milliseconds since 1970 UTC.
     */
    public long rcptTime;
    /**
     * Indicates whether message was correctly formatted or otherwise parseable.
     */
    protected boolean invalid;
    /**
     * Optional map of extracted data fields in this APRS message. May be null if no extensions present
     * in the message.
     */
    protected Map<Enum, Object> extensions = null;
    /**
     * The last digipeater or I-gate to forward this message.
     */
    private transient String lastDigipeater = null;

    /**
     * Constructor for partially initialized AX25Message.
     */
    protected AX25Message() {
    }

    /**
     * Constructor for AX25Message specifying the third-party network routing and receive time of the message.
     *
     * @param thirdParty The entire third-party routing path for this AX25Message, or null if this AX25message
     *                   is still on its original network.
     * @param rcptTime   The time the message was received by the system in Java standard milliseconds since 1970 UTC.
     */
    protected AX25Message(String thirdParty, long rcptTime) {
        if (thirdParty != null && thirdParty.length() > 0) {
            this.thirdParty = thirdParty;
        }
        this.rcptTime = rcptTime;
    }

    /**
     * Test if the specified part of the message body is strictly only ASCII digits.
     *
     * @param body String containing the message body
     * @param pos  starting index in the array to test
     * @param len  number of bytes to test
     * @return boolean true if all the bytes are ASCII digits
     */
    public static boolean onlyDigits(String body, int pos, int len) {
        for (int i = 0; i < len; i++) {
            char ch = body.charAt(pos + i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if the specified part of the message body is strictly only ASCII digits.
     *
     * @param body String containing the message body
     * @param pos  starting index in the array to test
     * @param len  number of bytes to test
     * @return boolean true if all the bytes are ASCII digits
     */
    public static boolean onlyDigitsOrPeriod(String body, int pos, int len) {
        for (int i = 0; i < len; i++) {
            char ch = body.charAt(pos + i);
            if ((ch < '0' || ch > '9') && ch != '.') {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if the specified part of the message body is strictly only ASCII digits.
     *
     * @param body byte array containing the message body
     * @param pos  starting index in the array to test
     * @param len  number of bytes to test
     * @return boolean true if all the bytes are ASCII digits
     */
    public static boolean onlyDigits(byte[] body, int pos, int len) {
        for (int i = 0; i < len; i++) {
            if (body[pos + i] < '0' || body[pos + i] > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if the specified part of the message body is only ASCII digits or
     * characters just after the digits (to support base+offset message codes in APRS).
     *
     * @param body byte array containing the message body
     * @param pos  starting index in the array to test
     * @param len  number of bytes to test
     * @return boolean true if all the bytes are in the 16-byte block containing ASCII digits
     */
    protected static boolean onlyDigitsPlus(byte[] body, int pos, int len) {
        for (int i = 0; i < len; i++) {
            if (body[pos + i] < '0' || body[pos + i] > '?') {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if the specified part of the message body is strictly only ASCII digits or space characters.
     *
     * @param body byte array containing the message body
     * @param pos  starting index in the array to test
     * @param len  number of bytes to test
     * @return boolean true if all the bytes are ASCII digits or spaces
     */
    protected static boolean onlyDigitsOrSpace(byte[] body, int pos, int len) {
        for (int i = 0; i < len; i++) {
            if ((body[pos + i] < '0' || body[pos + i] > '9') && ' ' != body[pos + i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if the specified part of the message body is strictly only period characters.
     *
     * @param body byte array containing the message body
     * @param pos  starting index in the array to test
     * @param len  number of bytes to test
     * @return boolean true if all the bytes are periods
     */
    protected static boolean onlyPeriods(byte[] body, int pos, int len) {
        for (int i = 0; i < len; i++) {
            if (body[pos + i] != '.') {
                return false;
            }
        }
        return true;
    }

    /**
     * Search a byte array (assumed to be an ASCII string) for a matching character.
     *
     * @param buf      byte array to search
     * @param bufLen   index of end of used part of buffer
     * @param matchCh  character value to search for in a forward search
     * @param startPos zero-based index to start searching at
     * @return first index of character occurrence after the start pos, or -1 if not found
     */
    public static int indexOf(byte[] buf, int bufLen, char matchCh, int startPos) {
        for (int i = startPos; i < bufLen; i++) {
            if (buf[i] == matchCh) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Search a byte array (assumed to be an ASCII string) for a matching ASCII string.
     *
     * @param buf      byte array to search
     * @param bufLen   index of end of used part of buffer
     * @param matchStr String value to search for in a forward search
     * @param startPos zero-based index to start searching at
     * @return first index of string occurrence after the start pos, or -1 if not found
     */
    public static int indexOf(byte[] buf, int bufLen, String matchStr, int startPos) {
        outerloop:
        for (int i = startPos; i < bufLen - matchStr.length(); i++) {
            if (buf[i] == matchStr.charAt(0)) {
                for (int j = 1; j < matchStr.length(); j++) {
                    if (buf[j + i] != matchStr.charAt(j)) {
                        // not the whole string
                        continue outerloop;
                    }
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * Extract the callsign of the original destination of this message.
     *
     * @param src        AX25Callsign of the source of the original AX25Frame
     * @param thirdParty String of the third-party routing of this message, or null if not routed over another network
     * @return source callsign String
     */
    public static String getOriginalSource(AX25Callsign src, String thirdParty) {
        if (thirdParty == null || thirdParty.length() == 0) {
            return src.toString();
        }

        // this code assumes TNC format

        // find end of source
        int srcPos = thirdParty.indexOf('>');

        return thirdParty.substring(0, srcPos);
    }

    /**
     * Extract the callsign of the original destination of this message.
     *
     * @param dest       AX25Callsign of the destination (tocall) of the original AX25Frame
     * @param thirdParty String of the third-party routing of this message, or null if not routed over another network
     * @return destination callsign String
     */
    public static String getOriginalDestination(AX25Callsign dest, String thirdParty) {
        if (thirdParty == null || thirdParty.length() == 0) {
            return dest.toString();
        }

        // this code assumes TNC format

        // find end of source
        int srcPos = thirdParty.indexOf('>', 3) + 1;

        // find end of initial destination
        int destPos = thirdParty.indexOf(',', srcPos + 2);

        return thirdParty.substring(srcPos, destPos);
    }

    /**
     * This is a more optimized version of String.split() that doesn't require
     * compiling and evaluating regular expression patterns to do it, thereby
     * saving chunks of transient heap (and probably some CPU time as well).
     *
     * @param line      the String to split at occurrences of the separator
     * @param separator the String delimiting substrings of the line
     * @return array of Strings split at the various points the separator appears
     */
    public static String[] split(String line, char separator) {
        int lastFoundPos = 0;
        int numHits = 0;
        int lineLen = line.length();
        while (lastFoundPos < lineLen) {
            int pos = line.indexOf(separator, lastFoundPos);
            if (-1 == pos) {
                break;
            }
            numHits++;
            lastFoundPos = pos + 1;
        }
        String[] answer = new String[numHits + 1];
        if (numHits == 0) {
            answer[0] = line;
        } else {
            lastFoundPos = 0;
            numHits = 0;
            while (true) {
                int pos = line.indexOf(separator, lastFoundPos);
                if (pos < 0) {
                    answer[numHits] = line.substring(lastFoundPos);
                    break;
                }
                answer[numHits++] = line.substring(lastFoundPos, pos);
                lastFoundPos = pos + 1;
            }
        }
        return answer;
    }

    /**
     * Test if the Object o is a duplicate of this Message.
     *
     * @param o Object to compare against this message.
     * @return true if o is a Message with the same contents as this Message
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AX25Message ax25message = (AX25Message) o;

        if (timestamp != ax25message.timestamp) return false;
        if (originatingCallsign == null) {
            if (ax25message.originatingCallsign != null) {
                return false;
            }
        } else if (!originatingCallsign.equals(ax25message.originatingCallsign)) {
            return false;
        }
        return bodyEquals(ax25message);
    }

    /**
     * Compare the contents of the body of the message, reporting if they match. This method
     * must be overridden by concrete subclasses, but will only be called when the Class of the
     * other message equals the Class of this message.
     *
     * @param other another AX25Message to compare against
     * @return boolean true if the body values are equivalent
     */
    abstract protected boolean bodyEquals(AX25Message other);

    /**
     * Returns a hash code for this Message.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        int result = 0;
        if (originatingCallsign != null) {
            result = originatingCallsign.hashCode();
        }
        result = 31 * result + (int) timestamp;
        return result;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this object.
     */
    public int compareTo(AX25Message o) {
        if (PERMANENT == timestamp || PERMANENT == o.timestamp) {
            return 0; // permanent fits in anywhere
        }
        if (timestamp > o.timestamp) {
            return -1; // later in time, earlier in order
        } else if (timestamp < o.timestamp) {
            return +1;
        }
        int diff;
        if ((diff = originatingCallsign.compareTo(o.originatingCallsign)) == 0) {
            if (thirdParty == null) {
                if (o.thirdParty != null) {
                    diff = -1;
                }
            } else if (o.thirdParty == null) {
                diff = +1;
            } else {
                diff = thirdParty.compareTo(o.thirdParty);
            }
        }
        return diff;
    }

    /**
     * Descriptive text about this message, to be included in the toString() method's response.
     * This method may be overridden. Its default implementation returns an empty string.
     *
     * @return String describing the contents of this message
     * @see #toString()
     */
    public String paramString() {
        return "";
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return (thirdParty != null && thirdParty.length() > 0 ? "}" + thirdParty + ":" : "");
    }

    /**
     * Extract the originating station callsign for this AX25Message.
     *
     * @throws IllegalArgumentException if thirdParty string is provided but doesn't have a sender&gt;dest delimiter
     */
    public void extractSource() throws IllegalArgumentException {
        String thirdParty;
        if ((thirdParty = this.thirdParty) == null || thirdParty.length() == 0) {
            AX25Frame ax25Frame;
            if ((ax25Frame = this.ax25Frame) != null) {
                if (ax25Frame.sender != null) {
                    originatingCallsign = ax25Frame.sender.toString();
                }
                if (ax25Frame.dest != null) {
                    originatingDest = ax25Frame.dest.toString();
                }
            }
        } else {
            int pos;
            if ((pos = thirdParty.indexOf('>')) < 0) {
                throw new IllegalArgumentException("invalid path '" + thirdParty + "'");
            }
            originatingCallsign = thirdParty.substring(0, pos);
            int pos2;
            if ((pos2 = thirdParty.indexOf('.', pos + 1)) < 0) {
                pos2 = thirdParty.length();
            }
            originatingDest = thirdParty.substring(pos + 1, pos2);
        }
    }

    /**
     * Get the AX.25 frame from which this Message was extracted.
     *
     * @return transport-compatible AX25Frame for this Message
     */
    public final AX25Frame getAx25Frame() {
        return ax25Frame;
    }

    /**
     * Attach the AX.25 frame from which this Message was extracted.
     *
     * @param ax25Frame AX25Frame object containing the encoding of this Message
     */
    public final void setAx25Frame(AX25Frame ax25Frame) {
        if (ax25Frame == null) {
            throw new NullPointerException("setting null AX25Frame associated with this Message");
        }
        this.ax25Frame = ax25Frame;
    }

    /**
     * Return the callsign of the last digipeat station for this message.
     *
     * @param digipeaters array of AX25Callsign digipeater addresses in AX.25 frame
     * @return String station ID of last digipeat station, or empty string if received
     * directly from originating station
     */
    public String getLastDigipeat(AX25Callsign[] digipeaters) {
        if (lastDigipeater != null) {
            return lastDigipeater;
        }
        String s = AX25Frame.getLastDigi(digipeaters);
        if (s.length() > 0) {
            return lastDigipeater = s;
        }
        int thirdPartyLength;
        if ((s = thirdParty) == null || (thirdPartyLength = s.length()) == 0) {
            return lastDigipeater = "";
        }
        int srcPos = s.indexOf('>', 1) + 1;
        int nextPos, lastSrcPos = -1;
        while (srcPos < thirdPartyLength) {
            nextPos = s.indexOf(',', srcPos + 1);
            if (-1 == nextPos) {
                nextPos = thirdPartyLength;
            }
            if (s.charAt(nextPos - 1) == '*') {
                int hyphenPos = s.lastIndexOf('-', nextPos - 2);
                if (hyphenPos <= srcPos) {
                    if (!Character.isDigit(s.charAt(nextPos - 2))) {
                        return lastDigipeater = s.substring(srcPos, nextPos - 1);
                    }
                } else if (!Character.isDigit(s.charAt(hyphenPos - 1))) {
                    return lastDigipeater = s.substring(srcPos, nextPos - 1);
                }

                // must be the previous token (if any), because this one looks like a digipeat alias instead of a callsign
                if (lastSrcPos > 0) {
                    return lastDigipeater = s.substring(lastSrcPos, srcPos - 1);
                }
                return lastDigipeater = "";
            } else if ("TCPIP".regionMatches(0, s, srcPos, nextPos - srcPos) ||
                    "TCPXX".regionMatches(0, s, srcPos, nextPos - srcPos)) {
                // didn't use any pre-I-Gate digipeaters
                return lastDigipeater = "";
            }
            lastSrcPos = srcPos;
            srcPos = nextPos + 1;
        }
        return lastDigipeater = "";
    }

    /**
     * Return the callsign of the first digipeat station for this message. Note this code
     * considers a direct receive by an I-gate as the I-gate being the first "digipeater".
     *
     * @param digipeaters array of AX25Callsign digipeater addresses in AX.25 frame
     * @return String station ID of first digipeat station, or empty string if received
     * directly from originating station
     */
    public String getFirstDigipeat(AX25Callsign[] digipeaters) {
        String thirdParty1;
        if ((thirdParty1 = this.thirdParty) != null && thirdParty1.length() > 0) {
            int startPos = thirdParty1.indexOf(',', 3) + 1; // start after destination field
            int endPos; // find the end of first digi field
            if ((endPos = thirdParty1.indexOf(',', startPos + 2)) < 0) {
                endPos = thirdParty1.length();
            }
            int usedPos;
            if ((usedPos = thirdParty1.indexOf('*', endPos - 1)) > 0) { // some callsign end with '*' meaning it was used?
                if (usedPos == endPos - 1) {
                    endPos--;
                }
                if (endPos - startPos == 5 && (
                        thirdParty1.regionMatches(startPos, "TCPIP", 0, 5) || thirdParty1.regionMatches(startPos, "TCPXX", 0, 5))) {
                    return ""; // don't have any receive I-gate callsign to identify RF->Internet entry point
                } else if (thirdParty1.charAt(startPos) != 'q') {
                    return thirdParty1.substring(startPos, endPos);
                }
            } else {
                usedPos = thirdParty1.indexOf(",q");
                if (usedPos > 0) {
                    startPos = thirdParty1.lastIndexOf(',');
                    if (startPos > usedPos) {
                        return thirdParty1.substring(startPos + 1); // the receiving I-gate is the first relay
                    }
                }
            }
        }
        return AX25Frame.getFirstDigi(digipeaters);
    }

    /**
     * Return the callsign of the Nth digipeat station for this message. Note this code
     * considers a direct receive by an I-gate as the I-gate being the first "digipeater".
     * If the index is past all the third-party digipeaters and I-gate and this is an
     * RF packet, the RF digipeaters used by the transmitting I-gate will then be
     * iterated through. Note that RF digipeater aliases that are not marked has-been-repeated
     * will not be counted, nor will "q" codes in a third-party prefix.
     *
     * @param digipeaters array of AX25Callsign digipeater addresses in AX.25 frame
     * @param index       zero-based index into the list of digipeat aliases
     * @return String station ID of Nth digipeat station, or null if past end of used digipeat list
     */
    public String getNthDigipeat(AX25Callsign[] digipeaters, int index) {
        String thirdParty1;
        String token = null;
        if ((thirdParty1 = this.thirdParty) != null && thirdParty1.length() > 0) {
            // zeroth token is the original destination (tocall)
            int delimPos = thirdParty1.indexOf(',');
            int nextDelimPos;
            int preIGateDigis = 0;
            boolean hasTcpip = false;
            while ((nextDelimPos = thirdParty1.indexOf(',', delimPos + 1)) > 0) {
                if (thirdParty1.charAt(delimPos + 1) == 'q' ||
                        ((nextDelimPos - delimPos == 6 || (nextDelimPos - delimPos == 7 && thirdParty1.charAt(nextDelimPos - 1) == '*')) &&
                                (hasTcpip = (thirdParty1.regionMatches(delimPos + 1, "TCPIP", 0, 5) ||
                                        thirdParty1.regionMatches(delimPos + 1, "TCPXX", 0, 5))))) {
                    delimPos = nextDelimPos;
                    break;
                }
                preIGateDigis++;
                if (index-- <= 0) {
                    token = thirdParty1.substring(delimPos + 1, nextDelimPos);
                    break;
                }
                delimPos = nextDelimPos;
            }
            if (index <= 0) {
                if (token == null && preIGateDigis == 0 && !hasTcpip) {
                    token = thirdParty1.substring(delimPos + 1);
                }
                if (token != null) {
                    if (token.endsWith("*")) { // some callsign end with '*' meaning it was used?
                        token = token.substring(0, token.length() - 1);
                    }
                    return token;
                }
            }
        }
        token = AX25Frame.getNthDigi(digipeaters, index);
        if (token != null && token.length() == 0) {
            token = null;
        }
        return token;
    }

    /**
     * Test if the specified callsign is the first digipeat station for this message.
     *
     * @param digipeaters  array of AX25Callsign digipeater addresses in AX.25 frame
     * @param digiCallsign String callsign/SSID of digipeater
     * @return boolean true if the specified digipeater is the first digi for this message
     */
    public boolean hasThisFirstDigi(AX25Callsign[] digipeaters, String digiCallsign) {
        String thirdParty1;
        if ((thirdParty1 = this.thirdParty) != null && thirdParty1.length() > 0) {
            int startPos = thirdParty1.indexOf(',', 3) + 1; // start after destination field
            int endPos;
            if ((endPos = thirdParty1.indexOf(',', startPos + 2)) < 0) {  // find the end of first digi field
                endPos = thirdParty1.length();
            }
            int usedPos;
            if ((usedPos = thirdParty1.indexOf('*', endPos - 1)) > 0) { // some callsign end with '*' meaning first digi was used?
                final int len;
                if (usedPos == endPos - 1) {
                    len = endPos - 1 - startPos;
                } else {
                    len = endPos - startPos;
                }
                return len == digiCallsign.length() && digiCallsign.regionMatches(0, thirdParty1, startPos, len);
            } else {
                usedPos = thirdParty1.indexOf(",q");
                if (usedPos > 0) {
                    startPos = thirdParty1.lastIndexOf(',');
                    if (startPos > usedPos) {
                        return thirdParty1.length() - startPos == digiCallsign.length() &&
                                digiCallsign.regionMatches(0, thirdParty1, startPos, digiCallsign.length()); // the receiving I-gate is the first relay
                    }
                }
            }
        }
        return AX25Frame.getFirstDigi(digipeaters).equals(digiCallsign);
    }

    /**
     * Get the timestamp associated with this Message in milliseconds since
     * 1 Jan 1970 UTC.
     *
     * @return time message was received or the timestamp in the message, or
     * -1 if this is a permanent (non-timing-out message)
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Change the timestamp of this message. Intended only for use by the
     * Transmitter class when creating a time-stamped duplicate of a
     * periodically-repeated APRS Message.
     *
     * @param timestamp new time in Java milliseconds since epoch
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Report if this AX25Message contains weather information.
     *
     * @return boolean true if weather information in this AX25Message
     */
    abstract public boolean hasWeather();

    /**
     * Report if this AX25Message contains position data. The default implementation
     * returns false; position-reporting subclasses are expected to override this.
     *
     * @return boolean true if message contains position information
     */
    public boolean hasPosition() {
        return false;
    }

    /**
     * Get the callsign of the station that originated this message (not of any Tx-Igate relay).
     *
     * @return String callsign
     */
    public String getOriginatingCallsign() {
        return originatingCallsign;
    }

    /**
     * Set the originating callsign for this AX25Message. This should only be called on SendableMessages
     * to initialize them before transmission.
     *
     * @param originatingCallsign String of the originating station callsign of this message
     */
    public void setOriginatingCallsign(String originatingCallsign) {
        this.originatingCallsign = originatingCallsign;
    }

    /**
     * Get the destination address oeiginally specified bv the station that originated this message.
     *
     * @return String callsign
     */
    public String getOriginatingDest() {
        return originatingDest;
    }

    /**
     * Get the timestamp this AX25Message was received in milliseconds since
     * 1 Jan 1970 UTC.
     *
     * @return time message was received
     */
    public long getRcptTime() {
        return rcptTime;
    }

    /**
     * Change the receive time of this message. Intended only for use by the
     * Transmitter class when creating a time-stamped duplicate of a
     * periodically-repeated APRS Message.
     *
     * @param rcptTime new time in Java milliseconds since epoch
     */
    public void setRcptTime(long rcptTime) {
        this.rcptTime = rcptTime;
    }

    /**
     * Test if this message was flagged as invalid.
     *
     * @return boolean true if this message is marked as invalid
     */
    public boolean isInvalid() {
        return invalid;
    }

    /**
     * Mark if this message is invalid or not.
     *
     * @param invalid boolean true if message should be considered invalid or incorrect syntax
     */
    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    /**
     * Report the traffic-handling precedence for this message instance.
     * Expected to be overridden by subclasses that have precedence fields.
     *
     * @return Precedence level for this AX25Message
     */
    public Precedence getPrecedence() {
        return Precedence.ROUTINE;
    }

    /**
     * Creates and returns a copy of this AX25Message.
     *
     * @return a clone of this instance.
     * @see Cloneable
     */
    public AX25Message dup() {
        try {
            return (AX25Message) super.clone();    //To change body of overridden methods use File | Settings | File Templates.
        } catch (CloneNotSupportedException e) {
            throw new InternalError("unable to clone " + getClass().getName());
        }
    }

    /**
     * Store an extracted data element in the Message.
     *
     * @param key   Enum that identifies the particular data item
     * @param value the data value
     * @param <K>   any enum subclass
     * @param <V>   any Java object class
     */
    public <K extends Enum, V> void storeExtension(K key, V value) {
        Map<Enum, Object> extensions1;
        if ((extensions1 = extensions) == null && value != null) {
            extensions = extensions1 = new LinkedHashMap<>(8, 1.0F);
        }
        if (value != null) {
            extensions1.put(key, value);
        } else if (extensions1 != null) {
            extensions1.remove(key);
            if (extensions1.size() == 0) {
                extensions = null;
            }
        }
    }

    /**
     * Get a reference to the extension map that should not be modified.
     *
     * @return Map of extension data element (may be an empty Map)
     */
    public Map<Enum, Object> getReadOnlyExtensionMap() {
        if (extensions != null) {
            return extensions;
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * Get a particular extension value from this message.
     *
     * @param key Enum instance identifying the desired extension
     * @return value for that extension, or null if no value stored
     */
    public Object getExtension(Enum key) {
        if (extensions != null) {
            return extensions.get(key);
        }
        return null;
    }

    /**
     * Get the protocol family or families that this message corresponds to, so
     * ports that don't support all protocols will not forward inappropriate packets.
     *
     * @return array of supported ProtocolFamily enums
     */
    public Set<ProtocolFamily> getProtocols() {
        return RAW_AX25_ONLY;
    }

    /**
     * Test if this AX25Message came from an RF connection.
     *
     * @param maxDigis int maximum number of digipeat hops before we're not going to count it
     * @return Boolean.TRUE if a local Rf transmission, Boolean.FALSE if not RF-only, null if we can't tell
     */
    public Boolean isRf(int maxDigis) {
        Boolean answer = null;
        final AX25Frame f = ax25Frame;
        if (f == null ||
                (f.sourcePort != null &&
                        (f.sourcePort.hasCapability(Connector.CAP_IGATE) || !f.sourcePort.hasCapability(Connector.CAP_RF)))) {
            answer = Boolean.FALSE;
        } else if (thirdParty != null && thirdParty.length() > 0) {
            // must have been I-gated , therefore not local
        } else if (f.digipeaters != null) {
            int numDigis = 0;
            for (AX25Callsign digi : f.digipeaters) {
                String baseCallsign = digi.getBaseCallsign();
                if (baseCallsign.equals("TCPIP") || baseCallsign.equals("TCPXX")) {
                    answer = Boolean.FALSE;
                    break;
                }
                if (!digi.hasBeenRepeated()) {
                    // unused digipeat alias, so we only needed to go up to last digi
                    break;
                }
                char lastChar = baseCallsign.charAt(baseCallsign.length() - 1);
                if (lastChar > '0' && lastChar <= '7') {
                    continue; // don't count aliases
                }
                numDigis++;
            }
            if (numDigis > maxDigis) {
                if (answer == null) {
                    answer = Boolean.FALSE;
                }
            } else {
                answer = Boolean.TRUE;
            }
        } else {
            answer = Boolean.TRUE;
        }
        return answer;
    }

    /**
     * Test if this message was sent directly (without any relay station).
     *
     * @return boolean true if direct, false if not
     */
    public boolean isDirect() {
        if (thirdParty != null) {
            return false;
        }
        return getLastDigipeat(ax25Frame.digipeaters).length() == 0;
    }

    /**
     * This enum defines the allowed traffic precedence levels for messages.
     */
    public enum Precedence {
        /**
         * Normal traffic with no preferential handling.
         */
        ROUTINE,
        /**
         * Health and welfare traffic. Not currently implemented in APRS, but reserved for future expansion.
         */
        WELFARE,
        /**
         * Station with reason to be specially monitored.
         */
        SPECIAL,
        /**
         * Time-sensitive traffic needing preferred handling.
         */
        PRIORITY,
        /**
         * Top-priority traffic preempting all other traffic, regarding life and safety issues.
         */
        EMERGENCY
    }
}
