package org.prowl.ax25;

/*
 * Copyright (C) 2011-2022 Andrew Pavlin, KA2DDO
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
 * This listener interface allows classes outside the org.ka2ddo.ax25 hierarchy
 * to be informed when connected sessions are updated.
 */
public interface ConnStateChangeListener {
    /**
     * Report that the row containing the specified pair of callsigns has been updated.
     * This is expected to be called from a thread other than the AWT dispatch thread.
     *
     * @param sender AX25Callsign of originator of session
     * @param dest   AX25Callsign of recipient of session
     */
    void updateConnStateRow(AX25Callsign sender, AX25Callsign dest);

    /**
     * Report that a ConnState session has been added or removed from the
     * {@link AX25Stack}, but we don't know which row number it is.
     * This is expected to be called from a thread other than the AWT dispatch thread.
     */
    void updateWholeConStateTable();
}
