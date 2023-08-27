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
 * This class provides an association between a frame and a PortConnector for transmission.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public class FrameWrapper implements AX25FrameSource {
    private final AX25Frame frame;
    private final FrameState state;
    private final Connector port;

    /**
     * Wrap an AX25Frame with information to direct it to a single Coonector port.
     *
     * @param frame AX25Frame to transmit
     * @param state FrameState object to be updated when the frame is transmitted, or null if statistics not desired
     * @param port  Connector to send the frame through
     */
    public FrameWrapper(AX25Frame frame, FrameState state, Connector port) {
        this.frame = frame;
        this.state = state;
        this.port = port;
    }

    /**
     * Get one or more AX25Frames of the data to transmit.
     *
     * @param incrementXmtCount indicate whether the transmit counter (used to cycle through
     *                          proportional pathing) should be incremented
     * @param protocolId        indicate the protocol to generate this frame for (not relevant for
     *                          digipeated frames)
     * @param senderCallsign    String of local callsign sending this message (may be ignored if digipeating
     *                          a message from another station)
     * @return array of AX25Frame objects to transmit (may be zero-length)
     */
    public AX25Frame[] getFrames(boolean incrementXmtCount, ProtocolFamily protocolId, String senderCallsign) {
        if (state != null && state.alreadyDigipeated) {
            return NO_FRAMES;
        }
        return frame.getFrames(incrementXmtCount, protocolId, senderCallsign);
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
        return port;
    }

    /**
     * Generate a String representation of this FrameWrapper.
     *
     * @return descriptive String
     */
    @Override
    public String toString() {
        return "FrameWrapper[" + state + ',' + frame + " to " + port + ']';
    }
}
