package org.prowl.ax25;

/*
 * Copyright (C) 2011-2018 Andrew Pavlin, KA2DDO
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

/**
 * This interface specifies an object that can queue AX.25 frames for transmission through
 * Connectors.
 *
 * @author Andrew Pavlin, KA2DDO
 * @see Connector
 * @see AX25Frame
 */
public interface Transmitting {

    /**
     * Queue the specified frame source for transmission over the specified (or all, if not
     * specified) transmit-enabled Connectors.
     *
     * @param entry AX25FrameSource of the frame to be transmitted
     */
    void queue(AX25FrameSource entry);

    /**
     * Queue the specified frame source for transmission over the specified (or all, if not
     * specified) transmit-enabled Connectors.
     *
     * @param entry      AX25FrameSource of the frame to be transmitted
     * @param timeToSend long time in milliseconds since Unix epoch when packet is to be dequeued and transmitted
     */
    void delayedQueue(AX25FrameSource entry, long timeToSend);

    /**
     * Test if this callsign is addressed to the local station.
     *
     * @param destCallsign String of AX.25 callsign-SSID to test as a destination
     * @return boolean true if this callsign is for the local station
     */
    boolean isLocalDest(String destCallsign);

    /**
     * Get the locally-originated message retransmit count.
     *
     * @return default retransmit count
     */
    int getRetransmitCount();
}
