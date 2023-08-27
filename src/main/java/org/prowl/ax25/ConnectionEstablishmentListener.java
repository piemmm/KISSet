package org.prowl.ax25;

/*
 * Copyright (C) 2011-2013 Andrew Pavlin, KA2DDO
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
 * This interface provides an asynchronous callback for requests to
 * open a AX.25 I-frame connection to another station. Each of these methods can
 * only be called once for any given session.
 */
public interface ConnectionEstablishmentListener {
    /**
     * Report that the requested connection has been successfully established.
     *
     * @param sessionIdentifier identifier of the particular connection
     * @param conn              the ConnState object from which communications streams can be obtained
     */
    void connectionEstablished(Object sessionIdentifier, ConnState conn);

    /**
     * Report that the requested connection could not be established.
     *
     * @param sessionIdentifier identifier of the particular connection
     * @param reason            object explaining why the connection could not be established
     */
    void connectionNotEstablished(Object sessionIdentifier, Object reason);

    /**
     * Report that the established connection was shut down normally.
     *
     * @param sessionIdentifier identifier of the particular connection
     * @param fromOtherEnd      boolean true if other end initiated the close
     */
    void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd);

    /**
     * Report that the established connection was closed abnormally.
     *
     * @param sessionIdentifier identifier of the particular connection
     * @param reason            object explaining why the connection was lost
     */
    void connectionLost(Object sessionIdentifier, Object reason);

}
