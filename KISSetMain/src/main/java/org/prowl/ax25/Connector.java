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
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class defines the generic API for an object that connects a
 * real-time data stream to this application.
 *
 * @author Andrew Pavlin, KA2DDO
 */
abstract public class Connector {
    /**
     * A port that is capable of transmitting AX.25 (or equivalent) data frames.
     */
    public static final int CAP_XMT_PACKET_DATA = 1;
    /**
     * A port that is capable of receiving AX.25 (or equivalent) data frames.
     */
    public static final int CAP_RCV_PACKET_DATA = 2;
    /**
     * A port with a connection to the Internet and APRS-IS backbone.
     */
    public static final int CAP_IGATE = 4;
    /**
     * A port capable of consuming NMEA-0183 GPS data.
     */
    public static final int CAP_GPS_DATA = 8;
    /**
     * A port capable of sending NMEA-0183 waypoint data.
     */
    public static final int CAP_WAYPOINT_SENDER = 16;
    /**
     * A port that can both send and receive data simultaneously. For example, most TNC
     * ports will not specify this (because the associated radio cannot transmit and receive
     * simultaneously on the same channel), but an Internet connection may specify this
     * capability.
     */
    public static final int CAP_FULL_DUPLEX = 32;
    /**
     * A port that can provide weather data through the WeatherDistributor.
     */
    public static final int CAP_WEATHER = 64;
    /**
     * A port that has local RF access.
     */
    public static final int CAP_RF = 128;
    /**
     * A port that can speak the OpenTRAC protocol.
     */
    public static final int CAP_OPENTRAC = 256;
    /**
     * A port that has HF radio access (i.e., low bandwidth, excessive geographical coverage). This
     * implies 300 baud for APRS RF bit rate.
     */
    public static final int CAP_HF = 512;
    /**
     * A port that can use Kenwood radio control protocol to alter the settings of the attached radio.
     */
    public static final int CAP_KENWOOD_CMD = 1024;
    /**
     * A port that can transmit or receive arbitrary raw AX.25 packets (not just APRS or OpenTRAC).
     */
    public static final int CAP_RAW_AX25 = 2048;
    /**
     * A port doing 9600 baud RF data rate instead of the usual 1200.
     */
    public static final int CAP_FAST_RF_9600 = 4096;
    /**
     * A port processing ADS-B data (not APRS or OpenTRAC).
     */
    public static final int CAP_ADSB = 8192;
    private static final ArrayList<FrameListener> frameListeners = new ArrayList<>();
    /**
     * Statistics about this PortConnector.
     */
    protected PortStats stats = new PortStats();

    /**
     * Add an object that wants to be informed of incoming raw AX.25 frames.
     *
     * @param l FrameListener to add
     */
    public static synchronized void addFrameListener(FrameListener l) {
        for (FrameListener f : frameListeners) {
            if (f == l) {
                return;
            }
        }
        frameListeners.add(l);
    }

    /**
     * Remove an object that used to be informed about incoming raw AX.25 frames.
     *
     * @param l FrameListener to unregister
     */
    public static synchronized void removeFrameListener(FrameListener l) {
        for (Iterator<FrameListener> it = frameListeners.iterator(); it.hasNext(); ) {
            FrameListener f = it.next();
            if (f == l) {
                it.remove();
                return;
            }
        }
    }

    /**
     * Send an AX.25 frame to all the listeners expecting to process raw frames,
     *
     * @param frame                    AX25Frame to process
     * @param rcptTimeInMsecSinceEpoch time since Unix epoch when frame started arriving
     */
    public static void fireConsumeFrame(AX25Frame frame, long rcptTimeInMsecSinceEpoch) {
        frame.rcptTime = rcptTimeInMsecSinceEpoch;
        ArrayList<FrameListener> frameListeners1 = frameListeners;
        for (int i = 0; i < frameListeners1.size(); i++) {
            frameListeners1.get(i).consumeFrame(frame);
        }
    }

    /**
     * Extract an AX.25 frame from a byte array and send it to all the listeners expecting to process raw frames,
     *
     * @param buf                      byte array supposedly containing an AX.25 frame
     * @param offset                   zero-based index into byte array where frame starts
     * @param length                   number of consecutive bytes in buffer that make up the frame
     * @param rcptTimeInMsecSinceEpoch time since Unix epoch when frame started arriving
     * @return the AX25Frame created from the specified bytes, or null if a frame could not be decoded
     */
    protected AX25Frame fireConsumeFrame(byte[] buf, int offset, int length, long rcptTimeInMsecSinceEpoch, AX25Stack stack) {
        AX25Frame frame;
        if ((frame = AX25Frame.decodeFrame(buf, offset, length, stack)) != null) { // did we get a valid frame?
            frame.sourcePort = this;
            fireConsumeFrame(frame, rcptTimeInMsecSinceEpoch);
        }
        return frame;
    }

    /**
     * Get the current statistics for this PortConnector instance.
     *
     * @return PortStats for this port.
     */
    public PortStats getStats() {
        return stats;
    }

    /**
     * Shut down this port connection. Expected to be overridden by sub-classes. Must be
     * idempotent (may be called repeatedly on an already-closed Connector).
     */
    public void close() {
    }

    /**
     * Clean up a Connector when the object is garbage-collected.
     *
     * @throws Throwable if any unhandled  problem occurs during cleanup
     */
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Reports whether this Connector has an open connection to its port.
     *
     * @return boolean true if Connector is open
     */
    abstract public boolean isOpen();

    /**
     * For ports that have an AX.25 address (callsign), report the callsign
     * associated with the port (used for transmissions initiated through this port
     * instead of digipeated). By default, returns null. Subclasses are expected
     * to override this.
     *
     * @return callsign String, or null if no associated callsign
     */
    public String getCallsign() {
        return null;
    }

    /**
     * Specify what capabilities a port of this type has. By default,
     * returns a zero bitmask (not capable of anything). Expected to be overridden
     * by subclasses.
     *
     * @return bitmask of capability flags
     * @see #CAP_RCV_PACKET_DATA
     * @see #CAP_XMT_PACKET_DATA
     * @see #CAP_FAST_RF_9600
     * @see #CAP_FULL_DUPLEX
     * @see #CAP_GPS_DATA
     * @see #CAP_HF
     * @see #CAP_IGATE
     * @see #CAP_KENWOOD_CMD
     * @see #CAP_OPENTRAC
     * @see #CAP_ADSB
     * @see #CAP_RAW_AX25
     * @see #CAP_RF
     * @see #CAP_WAYPOINT_SENDER
     * @see #CAP_WEATHER
     */
    public int getCapabilities() {
        return 0;
    }

    /**
     * Test if this Connector has the specified capability or capabilities.
     *
     * @param capMask bitmask of capabilities to be tested for
     * @return boolean true if this port has all the specified capabilities in its capability set
     * @see Connector#CAP_ADSB
     * @see Connector#CAP_RCV_PACKET_DATA
     * @see Connector#CAP_XMT_PACKET_DATA
     * @see Connector#CAP_FAST_RF_9600
     * @see Connector#CAP_FULL_DUPLEX
     * @see Connector#CAP_GPS_DATA
     * @see Connector#CAP_HF
     * @see Connector#CAP_IGATE
     * @see Connector#CAP_KENWOOD_CMD
     * @see Connector#CAP_OPENTRAC
     * @see Connector#CAP_RAW_AX25
     * @see Connector#CAP_RF
     * @see Connector#CAP_WAYPOINT_SENDER
     * @see Connector#CAP_WEATHER
     */
    public boolean hasCapability(int capMask) {
        return (getCapabilities() & capMask) == capMask;
    }

    /**
     * Report what type of traffic comes from this connector. By default, all
     * Connector subclasses provide realtime data unless they override this method.
     *
     * @return type of traffic sourced by this incoming Connector
     */
    public ConnectorType getType() {
        return ConnectorType.REALTIME;
    }

    /**
     * Type of Connector, used in filtering and deciding whether to digipeat or I-gate
     * traffic sourced from this connector.
     *
     * @author Andrew Pavlin, KA2DDO
     */
    public enum ConnectorType {
        /**
         * This is a source for real realtime traffic and should be displayed, logged,
         * and digipeated/I-gated as appropriate.
         */
        REALTIME,
        /**
         * This is a source for historical (playback) traffic, and should be displayed,
         * but not logged or digipeated/I-gated.
         */
        PLAYBACK,
        /**
         * This is a source of bogus data, and should be displayed (but filtered as realtime),
         * but not logged or digipeated/I-gated.
         */
        BOGUS_RT
    }

    /**
     * A data structure recording throughput statistics for its containing PortConnector instance.
     */
    public static class PortStats implements Cloneable, Serializable {
        private static final long serialVersionUID = -3782183103570324522L;
        /**
         * Running counter of received frames on this PortConnector.
         */
        public int numRcvFrames = 0;
        /**
         * Running counter of received bytes on this PortConnector.
         */
        public long numRcvBytes = 0;
        /**
         * Running counter of transmitted frames on this PortConnector.
         */
        public int numXmtFrames = 0;
        /**
         * Running counter of transmitted bytes on this PortConnector.
         */
        public long numXmtBytes = 0;
        /**
         * Running counter of defective received frames.
         */
        public int numBadRcvFrames = 0;
        /**
         * Running counter of transmission failures.
         */
        public int numBadXmtFrames = 0;
        /**
         * Number of times data loss (due to overruns) was detected.
         */
        public int numDataOverrunLosses = 0;

        /**
         * Reset all the statistics counters to zero.
         */
        public void clear() {
            numRcvBytes = 0;
            numRcvFrames = 0;
            numXmtBytes = 0;
            numXmtFrames = 0;
            numBadRcvFrames = 0;
            numBadXmtFrames = 0;
            numDataOverrunLosses = 0;
        }

        /**
         * Reset only the counters of successful operations.
         */
        public void clearGoodCounts() {
            numRcvBytes = 0;
            numRcvFrames = 0;
            numXmtBytes = 0;
            numXmtFrames = 0;
        }

        /**
         * Make a deep copy of this PortStats object.
         *
         * @return duplicate PortStats object with counter values as of the time dup() was called
         */
        public PortStats dup() {
            try {
                return (PortStats) clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError("unable to clone PortStats");
            }
        }
    }
}
