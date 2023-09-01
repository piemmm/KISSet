package org.prowl.ax25;
/*
 * Copyright (C) 2011-2021 Andrew Pavlin, KA2DDO
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
 * Define the range over which the tagged packet should be transmitted.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public enum Scope {
    /**
     * Packet should not be transmitted at all, but was created only to display information on the local screen.
     */
    PRIVATE,
    /**
     * Packet should be transmitted only through RF ports and should be tagged in the digipeater list as RFONLY and NOGATE
     * (to prevent I-gates from forwarding the packet to the Internet).
     */
    LOCAL,
    /**
     * Packet can be transmitted through both RF and APRS-IS (Internet) ports, and the digipeater list won't contain RFONLY or NOGATE,
     */
    GLOBAL
}
