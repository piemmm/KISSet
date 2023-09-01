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
 * Interface declaring an API for parsing an AX25Frame into a particular protocol (PID)'s
 * decoded message.
 *
 * @author Andrew Pavlin, KA2DDO
 */
@FunctionalInterface
public interface AX25Parser {
    /**
     * Parse a message to the appropriate object class.
     *
     * @param body         byte array containing the message to be parsed
     * @param src          AX25Callsign of the sending station
     * @param dest         AX25Callsign of the destination (probably an APRS alias)
     * @param digipeaters  array of AX25Callsigns for RF digipeaters, or null if none
     * @param rcvTimestamp the time in Java/Unix milliseconds since midnight Jan 1, 1970 UTC when this
     *                     message was actually received (as opposed to any timestamp that might be
     *                     embedded in the message body)
     * @param connector    Connector over which the message was received (null if from a file)
     * @return the decoded Message (if not decipherable, a DefaultMessage is returned)
     */
    AX25Message parse(byte[] body, AX25Callsign src, AX25Callsign dest, AX25Callsign[] digipeaters, long rcvTimestamp, Connector connector);
}