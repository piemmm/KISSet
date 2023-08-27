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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * This class defines 1 AX.25 packet, as defined by the <a href="https://www.ax25.net/AX25.2.2-Jul%2098-2.pdf" target="ax.25">AX.25 Link Level Protocol specification,
 * version 2.2</a>. Note that the Comparable interface is simply sorting
 * by rcptTime.
 *
 * @author Andrew Pavlin, KA2DDO
 * @version 2.2
 */
public class AX25Frame implements Serializable, AX25FrameSource, Comparable<AX25Frame> {
    /**
     * The bitmask to extract the frametype bits from the ctl byte of the frame.
     *
     * @see #ctl
     */
    public static final int MASK_FRAMETYPE = 0x03;
    /**
     * Numeric code for information (I) frame type.
     */
    public static final int FRAMETYPE_I = 0;
    /**
     * Numeric code for supervisory (S) frame type.
     */
    public static final int FRAMETYPE_S = 1;
    /**
     * Numeric code for unnumbered (U) frame type.
     */
    public static final int FRAMETYPE_U = 3;
    /**
     * Bitmask to extract supervisory (S) frame subtype from the ctl byte.
     *
     * @see #ctl
     */
    public static final int MASK_STYPE = 0x0C;
    /**
     * Bit shift to get least significant bit of S frame subtype into least significant bit of integer.
     */
    public static final int SHIFT_STYPE = 2;
    /**
     * Unshifted S frame subtype for Receive Ready frame.
     *
     * @see #MASK_STYPE
     */
    public static final int STYPE_RR = 0x00;
    /**
     * Unshifted S frame subtype for Receive Not Ready frame.
     *
     * @see #MASK_STYPE
     */
    public static final int STYPE_RNR = 0x04;
    /**
     * Unshifted S frame subtype for Reject frame.
     *
     * @see #MASK_STYPE
     */
    public static final int STYPE_REJ = 0x08;
    /**
     * Unshifted S frame subtype for Selective Reject frame.
     *
     * @see #MASK_STYPE
     */
    public static final int STYPE_SREJ = 0x0C;
    /**
     * Bitmask to extract unnumbered (U) frame subtype from the ctl byte.
     *
     * @see #ctl
     */
    public static final int MASK_UTYPE = 0xEC;
    /**
     * Bitmask to extract poll/final bit from unnumbered (U) frame ctl byte, mod 8 format.
     *
     * @see #ctl
     */
    public static final int MASK_U_P = 0x10;
    /**
     * Bitmask to extract poll/final bit from I/S frame 2nd ctl byte, mod 128 format.
     *
     * @see #ctl2
     */
    public static final int MASK_U_P128 = 0x01;
    /**
     * Bit shift to get least significant bit of U frame subtype into least significant bit of integer.
     */
    public static final int SHIFT_UTYPE = 2;
    /**
     * Unshifted U frame subtype for Unnumbered Information (UI).
     */
    public static final int UTYPE_UI = 0x00;
    /**
     * Unshifted U frame subtype for Disconnected Mode (DM).
     */
    public static final int UTYPE_DM = 0x0C;
    /**
     * Unshifted U frame subtype for Set Asynchronous Balanced Mode (SABM). Only allowed window sizing for AX.25 V2.0 stations.
     */
    public static final int UTYPE_SABM = 0x2C;
    /**
     * Unshifted U frame subtype for Disconnect (DISC).
     */
    public static final int UTYPE_DISC = 0x40;
    /**
     * Unshifted U frame subtype for Unnumbered Acknowledge (UA).
     */
    public static final int UTYPE_UA = 0x60;
    /**
     * Unshifted U frame subtype for Set Asynchronous Balanced Mode Extended (SABME). Only usable between AX.25 V2.2 stations.
     *
     * @since 2.2
     */
    public static final int UTYPE_SABME = 0x6C;
    /**
     * Unshifted U frame subtype for obsolete Frame Reject (FRMR).
     *
     * @deprecated 2.0
     */
    public static final int UTYPE_FRMR = 0x84;
    /**
     * Unshifted U frame subtype for Exchange Identification (XID).
     */
    public static final int UTYPE_XID = 0xAC;
    /**
     * Unshifted U frame subtype for Test (TEST).
     */
    public static final int UTYPE_TEST = 0xE0;
    /**
     * Protocol ID for CCITT X.25 PLP (also used by the ROSE network).
     */
    public static final byte PID_X25_PLP = (byte) 0x01;
    /**
     * Protocol ID for Van Jacobson compressed TCP/IP packet, per RFC 1144.
     */
    public static final byte PID_VJC_TCPIP = (byte) 0x06;
    /**
     * Protocol ID for Van Jacobson uncompressed TCP/IP packet, per RFC 1144.
     */
    public static final byte PID_VJUC_TCPIP = (byte) 0x07;
    /**
     * Protocol ID for AX.25 segmentation fragment.
     */
    public static final byte PID_SEG_FRAG = (byte) 0x08;
    /**
     * Protocol ID for OpenTRAC.
     */
    public static final byte PID_OPENTRAC = (byte) 0x77;
    /**
     * Protocol ID for TEXNET datagram.
     */
    public static final byte PID_TEXNET = (byte) 0xC3;
    /**
     * Protocol ID for Link Quality Protocol.
     */
    public static final byte PID_LQP = (byte) 0xC4;
    /**
     * Protocol ID for Appletalk.
     */
    public static final byte PID_ATALK = (byte) 0xCA;
    /**
     * Protocol ID for Appletalk Address Resolution Protocol (ARP).
     */
    public static final byte PID_AARP = (byte) 0xCB;
    /**
     * Protocol ID for ARPA Internet Protocol.
     */
    public static final byte PID_IP = (byte) 0xCC;
    /**
     * Protocol ID for ARPA Internet Address Resolution Protocol (ARP).
     */
    public static final byte PID_IARP = (byte) 0xCD;
    /**
     * Protocol ID for FlexNet.
     */
    public static final byte PID_FLEXNET = (byte) 0xCE;
    /**
     * Protocol ID for NET/ROM.
     */
    public static final byte PID_NETROM = (byte) 0xCF;
    /**
     * Protocol ID for no level 3 protocol (also used for APRS).
     */
    public static final byte PID_NOLVL3 = (byte) 0xF0;
    /**
     * Protocol ID for escape code indicating second byte of PID (not supported).
     */
    public static final byte PID_ESCAPE = (byte) 0xFF;
    /**
     * SerialVersionUID when rcptTime was of type java.org.ka2ddo.util.Date.
     *
     * @see #rcptTime
     */
    static final long previousSerialVersionUID = 4260042831169759L;
    private static final long serialVersionUID = 3107587793401226132L;
    private static final Log LOG = LogFactory.getLog("AX25Frame");
    /**
     * Maximum number of digipeat addresses allowed in an AX.25 frame, according to the AX.25 spec.
     */
    private static final int MAX_DIGIS = 8;
    /**
     * Maximum number of digipeat addresses allowed in an AX.25 frame by this class. Should be 8 according to the AX.25 spec,
     * but someone is managing to send over-length packets.
     */
    private static final int OVERSIZED_MAX_DIGIS = 10;
    /**
     * String names for the different AX.25 frame types.
     */
    private static final String[] FRAMETYPES_S = {"I", "S", "I", "U"};
    /**
     * String names of supervisory (S) frame subtypes (indexed after shifting).
     *
     * @see #MASK_STYPE
     * @see #SHIFT_STYPE
     */
    private static final String[] STYPES_S = {"RR", "RNR", "REJ", "SREJ"};
    /**
     * String names of unnumbered (U) frame subtypes (indexed after shifting).
     *
     * @see #MASK_UTYPE
     * @see #SHIFT_UTYPE
     */
    private static final String[] UTYPES_S = {"UI", "?1", "?2", "DM", "?4", "?5", "?6", "?7", "?8", "?9", "?10", "SABM",
            "?12", "?13", "?14", "?15", "DISC", "?17", "?18", "?19", "?20", "?21", "?22", "?23", "UA", "?25", "?26", "SABME", "?28", "?29", "?30", "?31",
            "?32", "FRMR", "?34", "35?", "?36", "?37", "?38", "?39", "?40", "?41", "?42", "XID", "?44", "?45", "?46", "?47",
            "?48", "?49", "?50", "?51", "?52", "?53", "?54", "?55", "TEST", "?57", "?58", "?59", "?60", "?61", "?62", "?63"};
    /**
     * Hashmap of Information (I) or Unnumbered Information (UI) frame protocol ID to protocol name strings.
     *
     * @see #pid
     */
    private static final HashMap<Byte, String> PTYPES_S_hidden = new HashMap<Byte, String>();
    /**
     * Hashmap of Information (I) or Unnumbered Information (UI) frame protocol ID to protocol name strings.
     *
     * @see #pid
     */
    public static final Map<Byte, String> PTYPES_S = Collections.unmodifiableMap(PTYPES_S_hidden);

    static {
        PTYPES_S_hidden.put(PID_X25_PLP, "X.25-PLP");
        PTYPES_S_hidden.put(PID_VJC_TCPIP, "VJC-TCP/IP");
        PTYPES_S_hidden.put(PID_VJUC_TCPIP, "VJuc-TCP/IP");
        PTYPES_S_hidden.put(PID_SEG_FRAG, "seg_frag");
        PTYPES_S_hidden.put(PID_OPENTRAC, "OpenTRAC");
        PTYPES_S_hidden.put(PID_TEXNET, "TEXNET");
        PTYPES_S_hidden.put(PID_LQP, "LQP");
        PTYPES_S_hidden.put(PID_ATALK, "Appletalk");
        PTYPES_S_hidden.put(PID_AARP, "Appletalk-ARP");
        PTYPES_S_hidden.put(PID_IP, "IP");
        PTYPES_S_hidden.put(PID_IARP, "IP-ARP");
        PTYPES_S_hidden.put(PID_FLEXNET, "FlexNet");
        PTYPES_S_hidden.put(PID_NETROM, "NET/ROM");
        PTYPES_S_hidden.put(PID_NOLVL3, "No_LVL3");
        PTYPES_S_hidden.put(PID_ESCAPE, "-escape-");
    }

    /**
     * Callsign of the transmitting station (not of any intermediate digipeaters).
     */
    public AX25Callsign sender;
    /**
     * Callsign of the destination station, or some broadcast code with an alternate meaning (such as
     * APRS tocalls and Mic-E encoded latitude and status values).
     */
    public AX25Callsign dest;
    /**
     * Optional array of digipeater callsigns and aliases, if this frame should be digipeated.
     * May have up to 8 elements in the array.
     */
    public AX25Callsign[] digipeaters;
    /**
     * Pointer to the I/O port from which this frame was received.
     */
    public transient Connector sourcePort;
    /**
     * Bitmask identifying the frame type and subtype, and windowing position for connection-oriented
     * I and S frames.
     *
     * @see #getFrameType()
     * @see #getUType()
     * @see #getSType()
     */
    public byte ctl;
    /**
     * Extension of ctl when using 128-segment windowing.
     *
     * @see #ctl
     * @see #mod128
     */
    public byte ctl2;
    /**
     * Byte array containing the higher-level protocol payload for I and UI frames.
     */
    public byte[] body;
    /**
     * Indicates whether 128-segment windowing is used for I frame connections. If this is false,
     * the backwards-compatible 8-segment windowing is used.
     */
    public boolean mod128;
    /**
     * The time when this message was received in Java milliseconds since midnight, Jan 1 1970 UTC.
     */
    public long rcptTime;
    /**
     * The decoded APRS (or other protocol) message (if the AX25Frame contains a higher-level protocol). May be null.
     */
    public AX25Message parsedAX25Msg;
    /**
     * Indicate whether this is a command or a poll message, for AX.25 frame
     * types that include a P/C bit.
     */
    boolean isCmd;

    // unused UTYPE's: 0x04, 0x08, 0x20, 0x24, 0x28, 0x44, 0x48, 0x4C, 0x64, 0x68,
    //  0x80, 0x88, 0x8C, 0xA0, 0xA4, 0xA8, 0xC0, 0xC4, 0xC8, 0xCC, 0xE4, 0xE8, 0xEC
    /**
     * Reference to the raw packet itself.
     */
    byte[] rawPacket;
    /**
     * The one-byte code identifying how to interpret the body of I and UI frames.
     *
     * @see #PID_X25_PLP
     * @see #PID_VJC_TCPIP
     * @see #PID_VJUC_TCPIP
     * @see #PID_SEG_FRAG
     * @see #PID_OPENTRAC
     * @see #PID_TEXNET
     * @see #PID_LQP
     * @see #PID_ATALK
     * @see #PID_AARP
     * @see #PID_IP
     * @see #PID_IARP
     * @see #PID_FLEXNET
     * @see #PID_NETROM
     * @see #PID_NOLVL3
     * @see #PID_ESCAPE
     */
    private byte pid;

    /**
     * Create an empty AX25Frame initialized for a UI frame containing an APRS packet.
     */
    public AX25Frame() {
        // assume APRS until explicitly changed
        ctl = (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_UI);
        pid = PID_NOLVL3;
    }

    /**
     * Create a AX25Frame from a byte array presumed to contain an AX.25 protocol sequence.
     *
     * @param buf    byte array to read frame from
     * @param offset zero-based index into the array where the frame starts
     * @param length number of bytes making up the frame
     * @return structured AX25Frame object, or null if byte array doesn't have enough bytes for a frame
     */
    public static AX25Frame decodeFrame(byte[] buf, int offset, int length, AX25Stack stack) {
        //LOG.warn("Frame decode length:" + length+" bytes  offset:"+offset+"  buflen:"+buf.length);

        // This can happen if for some reason the KISS escaping breaks and you end up with direwolf getting upset and sending
        // you a 'cmd:' response. For now we ignore the frame. This might also happen if you have sent ascii ctrl chrs that should
        // not have been sent.
        if (length < 15) {
            if (new String(buf, offset, length).startsWith("cmd:")) {
                LOG.warn("KISS remote endpoint entered 'cmd:' mode for unknown reason");
            }
            LOG.warn("Frame too short! (ignored): length:" + length + " bytes  offset:" + offset + "  buflen:" + buf.length + "  :" + new String(buf, offset, length));
            return null;
        }

        AX25Frame f = new AX25Frame();
        f.rawPacket = new byte[length];
        System.arraycopy(buf, offset, f.rawPacket, 0, length);
        f.rcptTime = System.currentTimeMillis();
        f.dest = new AX25Callsign(buf, offset, length);
        offset += 7;
        length -= 7;
        f.sender = new AX25Callsign(buf, offset, length);
        offset += 7;
        length -= 7;
        initializeCmd(f);
        if ((buf[offset - 1] & 0x01) == 0) {
            AX25Callsign[] rptList = new AX25Callsign[OVERSIZED_MAX_DIGIS];
            int numRptrs = 0;
            do {
                AX25Callsign c = new AX25Callsign(buf, offset, length);
                offset += 7;
                length -= 7;
                rptList[numRptrs++] = c; // let it blow up if more than 8 digipeaters
            } while ((buf[offset - 1] & 0x01) == 0);
            if (numRptrs == OVERSIZED_MAX_DIGIS) {
                f.digipeaters = rptList;
            } else if (numRptrs > 0) {
                System.arraycopy(rptList, 0, f.digipeaters = new AX25Callsign[numRptrs], 0, numRptrs);
            }
        }
        f.ctl = buf[offset++];
        length--;

        if ((f.ctl & MASK_FRAMETYPE) != FRAMETYPE_U) {
            // test if this is potentially a mod128 link
            f.mod128 = stack.getStateOf(f.sender, f.dest) == ConnState.ConnType.MOD128;

            // get the rest of the message
            if (f.mod128) {
                // I or S frame in mod128
                f.ctl2 = buf[offset++];
                length--;
            }
        }
        if ((f.ctl & 0x01) == 0 || (f.ctl & (MASK_UTYPE | MASK_FRAMETYPE)) == (UTYPE_UI | FRAMETYPE_U)) {
            // I or UI frame
            f.pid = buf[offset++];
            length--;
        }
        System.arraycopy(buf, offset, f.body = new byte[length], 0, length);
        return f;
    }

    /**
     * Properly set the isCmd flag of a frame based on the {@link AX25Callsign#h_c} bits in
     * the sender and destination addresses.
     *
     * @param f AX25Frame to set
     */
    public static void initializeCmd(AX25Frame f) {
        f.isCmd = f.dest.h_c && !f.sender.h_c;
    }

    /**
     * Given a string name, get the numeric S-type value for that type of AX.25 frame.
     *
     * @param sTypeName String name of frame type
     * @return S-type numeric value, or -1 if no match found
     */
    public static int findSTypeByName(String sTypeName) {
        int sType = STYPES_S.length - 1;
        while (sType >= 0) {
            if (sTypeName.equals(STYPES_S[sType])) {
                break;
            }
            sType--;
        }
        return sType;
    }

    /**
     * Given a string name, get the numeric U-type value for that type of AX.25 frame.
     *
     * @param uTypeName String name of frame type
     * @return U-type numeric value, or -1 if no match found
     */
    public static int findUTypeByName(String uTypeName) {
        int uType = UTYPES_S.length - 1;
        while (uType >= 0) {
            if (uTypeName.equals(UTYPES_S[uType])) {
                break;
            }
            uType--;
        }
        return uType;
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
     * Get the first actual digipeated digipeater station callsign in the digipeater sequence.
     *
     * @param digipeaters array of AX25Callsigns for digipeating a message
     * @return String of first real callsign in sequence, or empty String if no actual digipeater callsign
     */
    public static String getFirstDigi(AX25Callsign[] digipeaters) {
        String first = "";
        if (digipeaters != null) {
            for (AX25Callsign r : digipeaters) {
                if (!r.hasBeenRepeated()) {
                    break;
                } else {
                    // doesn't matter if this is a real traced callsign or an alias (with no trace before it)
                    first = r.toString();
                    break;
                }
            }
        }
        return first;
    }

    /**
     * Get the Nth digipeated digipeater station callsign in the digipeater sequence.
     *
     * @param digipeaters array of AX25Callsigns for digipeating a message
     * @param index       zero-based index of digipeater to report
     * @return String of callsign in sequence, or null if run out of repeated aliases
     */
    public static String getNthDigi(AX25Callsign[] digipeaters, int index) {
        String digi = null;
        if (digipeaters != null && digipeaters.length > index) {
            AX25Callsign r = digipeaters[index];
            if (r.hasBeenRepeated()) {
                // doesn't matter if this is a real traced callsign or an alias (with no trace before it)
                digi = r.toString();
            }
        }
        return digi;
    }

    /**
     * Find the last callsign through which a frame has been digipeated.
     *
     * @param digipeaters array of digipeater callsigns
     * @return String callsign of last digipeater entry that is marked as used, or empty String if none used
     */
    public static String getLastDigi(AX25Callsign[] digipeaters) {
        String last = "";
        if (digipeaters != null) {
            for (AX25Callsign r : digipeaters) {
                if (!r.hasBeenRepeated()) {
                    break;
                } else {
                    if (!DigipeatAliasCatalog.isRelayAStep(r)) {
                        last = r.toString();
                    }
                }
            }
        }
        return last;
    }

    /**
     * Get the frame PID - remember this is a byte so convert by & 0xFF to stick it in an int.
     *
     * @return
     */
    public byte getPid() {
        return pid;
    }

    void setPid(byte pid) {
        this.pid = pid;
    }

    public byte[] getRawPacket() {
        return rawPacket;
    }

    /**
     * Generate a string describing the type of the frame.
     *
     * @return descriptive String
     */
    public String getFrameTypeString() {
        StringBuilder b = new StringBuilder();
        int type = ctl & MASK_FRAMETYPE;
        if (2 == type) {
            type = FRAMETYPE_I; // blank N(S) from type
        }
        b.append(FRAMETYPES_S[type]);
        switch (type) {
            case FRAMETYPE_S:
                b.append(' ').append(STYPES_S[getSType() >> SHIFT_STYPE]);
                break;
            case FRAMETYPE_U:
                b.append(' ').append(UTYPES_S[getUType() >> SHIFT_UTYPE]);
                break;
            default:
                break;
        }
        return b.toString();
    }

    /**
     * @return the body data of the frame.
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Get the type of this frame, as stored in the ctl byte.
     *
     * @return frame type code
     * @see #FRAMETYPE_I
     * @see #FRAMETYPE_S
     * @see #FRAMETYPE_U
     */
    public int getFrameType() {
        int type = ctl & MASK_FRAMETYPE;
        if (2 == type) {
            type = FRAMETYPE_I; // blank N(S) from type
        }
        return type;
    }

    /**
     * Get the transmission sequence number.
     *
     * @return sequence number
     * @throws IllegalStateException if this is not an I frame
     */
    public int getNS() {
        if ((ctl & 0x01) == FRAMETYPE_I) {
            // I frame
            if (mod128) {
                return (ctl & 0xFE) >> 1;
            } else {
                return (ctl & 0x0E) >> 1;
            }
        }
        throw new IllegalStateException("only I frames have NS");
    }

    /**
     * Sets the transmission sequence number.
     *
     * @param ns sequence number
     * @throws IllegalStateException if this is not an I frame
     */
    void setNS(int ns) {
        if ((ctl & 0x01) == FRAMETYPE_I) {
            // I frame
            if (mod128) {
                ctl = (byte) ((ctl & 0x01) | ((ns << 1) & 0xFE));
            } else {
                ctl = (byte) ((ctl & 0xF1) | ((ns << 1) & 0x0E));
            }
        } else {
            throw new IllegalStateException("only I frames have NS");
        }
    }

    /**
     * Get the reception sequence number.
     *
     * @return sequence number
     * @throws IllegalStateException if this is not an I frame
     */
    public int getNR() {
        if (((ctl & 0x01) == FRAMETYPE_I) ||
                ((ctl & MASK_FRAMETYPE) == FRAMETYPE_S)) {
            if (mod128) {
                return (ctl2 & 0xFE) >> 1;
            } else {
                return (ctl & 0xE0) >> 5;
            }
        }
        throw new IllegalStateException("only I or S frames have NR");
    }

    /**
     * Sets the reception sequence number.
     *
     * @param nr sequence number
     * @throws IllegalStateException if this is not an I frame
     */
    void setNR(int nr) {
        if (((ctl & 0x01) == FRAMETYPE_I) ||
                ((ctl & MASK_FRAMETYPE) == FRAMETYPE_S)) {
            if (mod128) {
                ctl2 = (byte) ((ctl2 & 0x01) | ((nr << 1) & 0xFE));
            } else {
                ctl = (byte) ((ctl & 0x1F) | ((nr << 5) & 0xE0));
            }
        } else {
            throw new IllegalStateException("only I or S frames have NR");
        }
    }

    /**
     * Get the Supervisory frame subtype.
     *
     * @return S frame subtype (masked but not bit-shifted from its position in ctl bitmask)
     * @see #STYPE_RR
     * @see #STYPE_RNR
     * @see #STYPE_REJ
     * @see #STYPE_SREJ
     */
    public int getSType() {
        if ((ctl & 0x03) == FRAMETYPE_S) {
            return (ctl & MASK_STYPE);
        }
        throw new IllegalStateException("only S frames have SType");
    }

    /**
     * Get poll bit.
     *
     * @return boolean state of poll bit
     */
    public boolean getP() {
        if ((ctl & 0x01) == 0) {
            // I frame
            if (mod128) {
                return (ctl2 & 0x01) != 0;
            } else {
                return (ctl & 0x10) != 0;
            }
        } else if ((ctl & MASK_FRAMETYPE) == 1) {
            // S frame
            if (mod128) {
                return (ctl2 & 0x01) != 0;
            } else {
                return (ctl & 0x10) != 0;
            }
        } else {
            // U frame
            return (ctl & 0x10) != 0;
        }
    }

    /**
     * Get Unordered frame subtype.
     *
     * @return U frame subtype (masked but not bit-shifted from its position in ctl bitmask)
     * @see #UTYPE_UI
     * @see #UTYPE_DM
     * @see #UTYPE_SABM
     * @see #UTYPE_DISC
     * @see #UTYPE_UA
     * @see #UTYPE_SABME
     * @see #UTYPE_FRMR
     * @see #UTYPE_XID
     * @see #UTYPE_TEST
     */
    public int getUType() {
        if ((ctl & MASK_FRAMETYPE) == FRAMETYPE_U) {
            return (ctl & MASK_UTYPE);
        }
        throw new IllegalStateException("only U frames have UType");
    }

    /**
     * Transmit this AX25Frame to an output byte stream.
     *
     * @param os OutputStream to write the frame to
     * @throws IOException if writing fails
     */
    public void write(OutputStream os) throws IOException {
/*
        if (dest.h_c == sender.h_c) {
            throw new IllegalArgumentException("using obsolete command/response bit in AX.25 frame = " + dest.h_c + ' ' + sender.h_c);
        }
*/
        dest.write(os, false);
        sender.write(os, digipeaters == null || digipeaters.length == 0);
        if (digipeaters != null && digipeaters.length > 0) {
            for (int relIdx = 0; relIdx < digipeaters.length; relIdx++) {
                digipeaters[relIdx].write(os, relIdx == digipeaters.length - 1);
            }
        }
        os.write(ctl);
        if (mod128) {
            os.write(ctl2);
        }
        if ((ctl & 0x01) == 0 || (ctl & (MASK_UTYPE | MASK_FRAMETYPE)) == (UTYPE_UI | FRAMETYPE_U)) {
            // I or UI frame
            os.write(pid);
        }
        if (body != null && body.length > 0) {
            os.write(body);
        }
    }

    /**
     * Create a deep copy of this frame.
     *
     * @return duplicate AX25Frame instance
     */
    public AX25Frame dup() {
        AX25Frame f = dupOnlyHeader();
        if (body != null) {
            f.body = new byte[body.length];
            System.arraycopy(body, 0, f.body, 0, body.length);
        }
        f.rcptTime = rcptTime;
        return f;
    }

    /**
     * Create a deep copy of this frame, excluding the body. This is useful for digipeating
     * messages received from APRS-IS, because it skips making a copy of the message body
     * that will immediately be discarded as part of the 3rd-party re-packaging.
     *
     * @return almost-duplicate AX25Frame instance
     */
    public AX25Frame dupOnlyHeader() {
        AX25Frame f = new AX25Frame();
        if (sender != null) {
            f.sender = sender.dup();
        }
        if (dest != null) {
            f.dest = dest.dup();
        }
        if (digipeaters != null) {
            f.digipeaters = new AX25Callsign[digipeaters.length];
            for (int i = 0; i < digipeaters.length; i++) {
                f.digipeaters[i] = digipeaters[i].dup();
            }
        }
        f.isCmd = isCmd;
        f.mod128 = mod128;
        f.ctl = ctl;
        f.ctl2 = ctl2;
        f.pid = pid;
        f.sourcePort = sourcePort;
        return f;
    }

    /**
     * Get the frames associated with this FrameSource (in this case, itself).
     *
     * @param incrementXmtCount indicate whether the transmit counter (used to cycle through
     *                          proportional pathing) should be incremented
     * @param protocolId        indicate the protocol to generate this frame for (not relevant for
     *                          digipeated frames)
     * @param senderCallsign    local sending callsign (ignored if frame already has
     *                          the callsign filled in)
     * @return one-element array point at this frame
     */
    public AX25Frame[] getFrames(boolean incrementXmtCount, ProtocolFamily protocolId, String senderCallsign) {
        if (sender == null && senderCallsign != null) {
            sender = new AX25Callsign(senderCallsign);
            sender.h_c = !dest.h_c;
        }
        if (getProtocols().contains(protocolId)) {
            return new AX25Frame[]{this};
        } else {
            return NO_FRAMES; // doesn't match the protocol
        }
    }

    /**
     * Get number of times frame will be retransmitted before inter-packet delay is increased.
     *
     * @return transmission count before interval increase
     */
    public int getNumTransmitsBeforeDecay() {
        return 1;
    }

    /**
     * Specify the Connector this message should be transmitted through.
     *
     * @return a specific Connector instance to transmit through, or null for all
     * applicable ports (Connector.CAP_XMT_PACKET_DATA and not rejecting
     * this specific packet [such as IGateConnectors shouldn't re-transmit
     * something received from the IGate])
     * @see Connector#CAP_XMT_PACKET_DATA
     */
    public Connector getConnector() {
        return null;
    }

    /**
     * Test if this AX25Frame came from an RF connection.
     *
     * @param maxDigis int maximum number of digipeat hops before we're not going to count it
     * @return Boolean.TRUE if a local Rf transmission, Boolean.FALSE if not RF-only, null if we can't tell
     */
    public Boolean isRf(int maxDigis) {
        Boolean answer = null;
        if ((sourcePort != null &&
                (sourcePort.hasCapability(Connector.CAP_IGATE) || !sourcePort.hasCapability(Connector.CAP_RF)))) {
            answer = Boolean.FALSE;
        } else if (digipeaters != null) {
            int numDigis = 0;
            for (AX25Callsign digi : digipeaters) {
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
        return getLastDigi(digipeaters).length() == 0;
    }

    /**
     * Set the command bits in the sender and destination fields.
     *
     * @param isCmd boolean true if this is a command frame, false if a response
     */
    public void setCmd(boolean isCmd) {
        if (isCmd) {
            dest.h_c = true;
            if (sender != null) {
                sender.h_c = false;
            }
        } else {
            dest.h_c = false;
            if (sender != null) {
                sender.h_c = true;
            }
        }
        this.isCmd = isCmd;
    }

    /**
     * Return a String representing this AX25Frame object.
     *
     * @return descriptive String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AX25Frame[");
        if (sender != null) {
            sb.append(sender);
        }
        sb.append('>');
        if (dest != null) {
            sb.append(dest);
        }

        if (digipeaters != null) {
            sb.append(',');
            sb.append(Arrays.toString(digipeaters));
        }
        sb.append(",ctl=");
        sb.append(Integer.toHexString(ctl & 0xFF));
        if (mod128) {
            sb.append(',');
            sb.append(Integer.toHexString(ctl2 & 0xFF));
        }
        sb.append(",pid=");
        sb.append(Integer.toHexString(pid & 0xFF));

        // If S or I frame then add the NR
        if ((ctl & 0x01) == FRAMETYPE_I || (ctl & MASK_FRAMETYPE) == FRAMETYPE_S) {
            sb.append(",NR=");
            sb.append(getNR());
        }

        // And NS for I frames
        if ((ctl & 0x01) == FRAMETYPE_I) {
            sb.append(",NS=");
            sb.append(getNS());
        }

        if (body != null) {
            sb.append(",#=");
            sb.append(body.length);
        } else {
            sb.append(",empty-body");
        }
        sb.append(']');
        return sb.toString();


       // return "AX25Frame[" + dest + '<' + sender + (digipeaters != null ? "," + Arrays.toString(digipeaters) : "") + ",ctl=" + Integer.toHexString(ctl & 0xFF) + ",pid=" + Integer.toHexString(pid & 0xFF) + (body != null ? ",#=" + body.length : ",empty-body") + ']';
    }

    /**
     * Produce an ASCIIfied version of frame body.
     *
     * @return String version of frame body
     */
    public String getAsciiFrame() {
        char[] chArray = new char[body.length];
        for (int i = 0; i < chArray.length; i++) {
            int chi = body[i] & 0xFF;
            if (chi < 0x1F) {
                chi |= 0x2400;
            }
            chArray[i] = (char) chi;
        }
        return new String(chArray);
    }

    /**
     * Get the raw frame body, unaltered.
     *
     * @return a raw byte frame body
     */
    public byte[] getByteFrame() {
        return body;
    }


    /**
     * Compute a checksum for this frame to allow efficiently identifying
     * duplicate frames. The checksum only covers the sender callsign and
     * the body, as its purpose is to prevent duplicate digipeating. Alas,
     * it won't work with duplicate transmit IGates, since they change the
     * the sending callsign to that of the IGate and inject a third-party
     * header into the body.
     *
     * @return int checksum of the current contents of this AX25Frame object
     */
    public int getChecksum() {
        // we're still doing Adler-32, but doing it inline without all the JNI calls and object malloc's
        //    of the java.org.ka2ddo.util.zip.Adler32 class
        int a = 1, b = 0;
        byte[] body;
        if (sender != null) {
            String senderCallsign = sender.getBaseCallsign();
            int callsignLen;
            if ((callsignLen = senderCallsign.length()) > 6) {
                callsignLen = 6; // force illegal callsigns to fit into AX.25 standards for checksumming
            }
            for (int i = 0; i < callsignLen; i++) {
                a += senderCallsign.charAt(i) << 1;
                b += a;
            }
            b += a * (6 - callsignLen); // for short base callsigns
            a += sender.get7thByte();
            b += a;
        }
        a += ctl & 0xFF;
        b += a;
        a += pid & 0xFF;
        b += a; // note we still haven't consumed enough bytes to need the modulo 65521 check yet
        if ((body = this.body) != null) {
            int bodyLen;
            if ((bodyLen = body.length) > 0) {
                int bodyStart = 0;
                AX25Message parsedAX25Msg;
                if ((parsedAX25Msg = this.parsedAX25Msg) != null && parsedAX25Msg.thirdParty != null) {
                    bodyStart = parsedAX25Msg.thirdParty.length() + 1; // assuming 3rd-party header is always ASCII
                }
                for (int i = bodyStart; i < bodyLen; i++) {
                    a += body[i] & 0xFF;
                    if (a >= 65521) {
                        a -= 65521;
                    }
                    b += a;
                    if (b >= 65521) {
                        b -= 65521;
                    }
                }
            }
        }
        return (b << 16) + a;
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
    public int compareTo(AX25Frame o) {
        return Long.signum(rcptTime - o.rcptTime);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof AX25Frame) && isDuplicate((AX25Frame) obj);
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return (sender != null ? sender.hashCode() : 0) + ctl + pid * 37 + Arrays.hashCode(body);
    }

    /**
     * Test if this frame has the same contents (at least as regards duplicate checking)
     * as the provided older frame.
     *
     * @param other AX25Frame to compare payloads with
     * @return boolean true if this frame should be treated as a duplicate for digipeating purposes
     */
    public boolean isDuplicate(AX25Frame other) {
        if (Objects.equals(sender, other.sender)) {
            if (ctl == other.ctl &&
                    pid == other.pid) {
                return Arrays.equals(body, other.body);
            }
        }
        return false;
    }

    /**
     * Estimate the number of bits needed to transmit this frame over RF in AX.25 standard physical layer (HDLC).
     *
     * @return bit count estimate
     */
    public int getEstimatedBitCount() {
        int frameType = getFrameType();
        byte[] body;
        int numBytes = 19 // destination + source + ctl + checksum + HDLC flags
                + (digipeaters != null ? 7 * digipeaters.length : 0) // digipeaters
                + (mod128 ? 1 : 0) // 2-byte control field
                + (frameType == FRAMETYPE_I || (frameType == FRAMETYPE_U && getUType() == UTYPE_UI) ? 1 : 0) // protocol ID
                + ((body = this.body) != null ? body.length : 0); // body only exists for I and some subtypes of U frames
        int numBits = 8 * numBytes;
        int stuffBits = 0;
        if (body != null) {
            // this formula assumes APRS packets, which are ASCII therefore MSB never set
            for (int i = body.length - 1; i >= 0; i--) {
                byte b = body[i];
                if (b == 0x01F || b == '>' || b == '?' || b == '_' || b >= '|') {
                    stuffBits++;
                }
            }
        }
        return numBits + stuffBits; // extra for assumed HDLC bit-stuffing
    }

    /**
     * Report whether this packet is strictly valid according to the AX.25 protocol specification,
     *
     * @return boolean true if packet is valid
     */
    public boolean isValid() {

        if (sender != null && dest != null) {
            if (digipeaters != null) {
                if (digipeaters.length > MAX_DIGIS) {
                    return false;
                } else {
                    boolean isDigied = true;
                    for (int i = 0; i < digipeaters.length; i++) {
                        if (!digipeaters[i].hasBeenRepeated()) {
                            isDigied = false;
                        } else if (!isDigied) {
                            // unused digi addresses before used ones, out of sequence
                            return false;
                        }
                    }
                }
            }
            // check packet type
            if (getFrameType() == FRAMETYPE_U) {
                int uType = (getUType() & 0xFF) >> SHIFT_UTYPE;
                if (UTYPES_S[uType].startsWith("?")) {
                    return false;
                }
                if (getUType() == UTYPE_UI) {
                    // recognized protocol?
                    if (PTYPES_S_hidden.get(pid) == null) {
                        return false;
                    }
                    return body != null && body.length != 0 && body.length <= 256;
                }
            } else if (getFrameType() == FRAMETYPE_I) {
                // recognized protocol?
                if (PTYPES_S_hidden.get(pid) == null) {
                    return false;
                }
                return body != null && body.length != 0 && body.length <= 256;
            }
            return true;
        }
        return false;
    }

    /**
     * Get the protocol family or families that this message corresponds to, so
     * ports that don't support all protocols will not forward inappropriate packets.
     *
     * @return array of supported ProtocolFamily enums
     */
    public Set<ProtocolFamily> getProtocols() {
        if (parsedAX25Msg != null) {
            return parsedAX25Msg.getProtocols();
        }
        return AX25Message.RAW_AX25_ONLY;
    }
}
