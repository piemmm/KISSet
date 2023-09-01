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
 * This interface defines a means by which an arbitrary handler can choose to accept
 * an inbound AX.25 connected-mode session request.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public interface ConnectionRequestListener {
    /**
     * Decide whether to accept the specified inbound AX.25 connected-mode session request. Note
     * that the state is not fully connected at the point of this call (so the called code can choose
     * to reject it), so the called code should register a {@link ConnectionEstablishmentListener} on the
     * ConnState to be informed when the connection is fully established if the called code chooses to
     * accept the connection request.
     *
     * @param state      ConnState object describing the session being built
     * @param originator AX25Callsign of the originating station
     * @param port       Connector through which the request was received
     * @return boolean true if request should be accepted, false if not
     * @see ConnState#listener
     */
    boolean acceptInbound(ConnState state, AX25Callsign originator, Connector port);

    /**
     * The connector needs to know the callsigns that are running services (we are listening for connections to)
     * and this method can check for this.
     *
     * @param callsign The callsign to check
     * @return true if the callsign is a local callsign
     */
    boolean isLocal(String callsign);
}
