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
 * This interface defines a listener for decoded AX.25 frames as freshly
 * received from an input port.
 *
 * @author Andrew Pavlin, KA2DDO
 */
@FunctionalInterface
public interface AX25FrameListener {
    /**
     * Receive an incoming frame from the specified input Connector. Note that
     * the frame will not have any decoded AX25Message associated with it yet.
     *
     * @param frame     AX25Frame that was received
     * @param connector Connector that was the source of the frame.
     */
    void consumeAX25Frame(AX25Frame frame, Connector connector);
}
