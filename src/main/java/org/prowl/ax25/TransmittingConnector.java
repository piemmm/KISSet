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

import java.io.IOException;

/**
 * This class extends the basic capabilities of being a port connector by
 * specifying the methods usable to transmit AX.25 frames through this
 * port.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public interface TransmittingConnector {
    /**
     * Reports whether this Connector has an open connection to its port.
     *
     * @return boolean true if Connector is open
     */
    boolean isOpen();

    /**
     * Get the current statistics for this PortConnector instance.
     *
     * @return PortStats for this port.
     */
    Connector.PortStats getStats();

    /**
     * Transmit an AX.25 frame through this port. May fail silently if port is not
     * configured for transmission (receive-only). May also queue for later transmission
     * if timeslotting is used. Should be thread-safe so that multiple simultaneous calls
     * do not interleave bytes of different frames.
     *
     * @param frame AX25Frame object to transmit
     * @throws IOException if transmit failed for any reason other than a receive-only port
     */
    void sendFrame(AX25Frame frame) throws IOException;

    /**
     * For ports that have an AX.25 address (callsign), report the callsign
     * associated with the port (used for transmissions initiated through this port
     * instead of digipeated). By default, returns empty string. Subclasses are expected
     * to override this.
     *
     * @return callsign String, or null if no associated callsign
     */
    String getCallsign();

    /**
     * Specify what capabilities a port of this type has.
     *
     * @return bitmask of capability flags
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
    int getCapabilities();

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
    boolean hasCapability(int capMask);

    /**
     * Get the bitmask of AX.25 protocols supported by this port.
     * Bit positions correspond to the {@link ProtocolFamily#ordinal()} value of
     * the {@link ProtocolFamily} enum.
     *
     * @return protocol bitmask, or 0 for no AX.25 protocols supported
     */
    int getAcceptableProtocolsMask();
}
