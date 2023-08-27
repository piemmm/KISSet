package org.prowl.ax25;
/*
 * Copyright (C) 2011-2021 Andrew Pavlin, KA2DDO
 * This file is part of YAAC.
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
 * This interface extends the {@link AX25Parser} interface to indicate it has a means
 * to distribute protocol-specific packets to interested listeners. Because the
 * listeners are protocol-specific, their registration method signatures are not defined
 * here; this is just the hand-off for the {@link AX25Stack} to send the
 * decoded packets to the protocol-specific distributor, which then handles any
 * casting to subclasses and distribution to registered listeners.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public interface AX25ParserWithDistributor extends AX25Parser {
    /**
     * Send this message (and its associated frame) to the APRS consumers.
     *
     * @param frame     AX25Frame containing the APRS message
     * @param parsedMsg AX25Message that was decoded from the frame
     */
    void processParsedAX25Packet(AX25Frame frame, AX25Message parsedMsg);
}
