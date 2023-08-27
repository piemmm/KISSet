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
 * This interface defines how a code segment waiting for a response message is
 * informed when the response is received. Such implementors should be registered
 * with the {@link AX25Stack} method {@link AX25Stack#addParsedAX25MessageListener(ParsedAX25MessageListener)}.
 *
 * @author Andrew Pavlin, KA2DDO
 */
@FunctionalInterface
public interface ParsedAX25MessageListener {
    /**
     * Delivers the next message received by YAAC that is some sort of parsed AX.25 higher-level message.
     *
     * @param pid AX.25 protocol ID
     * @param msg some subclass of AX25Message containing the message contents; the message should have
     *            an AX25Frame connected to it
     * @see AX25Frame
     * @see AX25Message#ax25Frame
     * @see AX25Message#getAx25Frame()
     */
    void parsedAX25MessageReceived(byte pid, AX25Message msg);
}
