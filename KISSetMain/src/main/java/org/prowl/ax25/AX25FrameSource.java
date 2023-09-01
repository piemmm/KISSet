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

/**
 * This interface specifies an object that can provide a fully-encoded AX.25
 * frame for transmission. The object will be queried whenever the transmission
 * queue decides to transmit the message. The
 * object is not required to return the same frame upon multiple queries (due to
 * retransmissions, etc.); this allows for updating beacons with mobile stations,
 * or doing proportional pathing.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public interface AX25FrameSource {
    /**
     * Singleton array of no AX.25 frame objects, suitable for a response where no frames
     * can be returned or generated.
     */
    AX25Frame[] NO_FRAMES = new AX25Frame[0];

    /**
     * Get one or more AX25Frames of the data to transmit.
     *
     * @param incrementXmtCount indicate whether the transmit counter (used to cycle through
     *                          proportional pathing) should be incremented
     * @param protocolId        indicate the protocol to generate this frame for (not relevant for
     *                          digipeated frames)
     * @param senderCallsign    String of local callsign sending this message (may be ignored if digipeating
     *                          a message from another station)
     * @return array of AX25Frame objects to transmit (may be zero-length), or null indicating nothing to transmit in the specified protocol
     */
    AX25Frame[] getFrames(boolean incrementXmtCount, ProtocolFamily protocolId, String senderCallsign);

    /**
     * Get number of times frame will be retransmitted before inter-packet delay is increased.
     *
     * @return transmission count before interval increase
     */
    int getNumTransmitsBeforeDecay();

    /**
     * Specify the Connector this message should be transmitted through.
     *
     * @return a specific Connector instance to transmit through, or null for all
     * applicable ports (Connector.CAP_XMT_PACKET_DATA and not rejecting
     * this specific packet [such as I-Gate Connectors shouldn't re-transmit
     * something received from the I-Gate])
     * @see Connector#CAP_XMT_PACKET_DATA
     */
    Connector getConnector();
}
