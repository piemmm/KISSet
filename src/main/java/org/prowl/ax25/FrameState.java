package org.prowl.ax25;
/*
 * Copyright (C) 2011-2016 Andrew Pavlin, KA2DDO
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
 * The class describes the digipeat status of an associated AX25Frame.
 */
public class FrameState {
    /**
     * Indicate whether the associated frame has already been digipeated.
     */
    public boolean alreadyDigipeated;
    /**
     * Record when the frame was digipeated.
     */
    public long when;

    /**
     * Create a FrameState for the current time, already digipeated.
     */
    public FrameState() {
        this.when = System.currentTimeMillis();
        this.alreadyDigipeated = true;
    }

    /**
     * Create a FrameState for the specified time, not yet digipeated.
     *
     * @param now time of frame in milliseconds since Jan 1 1970 UTC
     */
    public FrameState(long now) {
        this.when = now;
    }

    /**
     * Report a description of this FrameState object.
     *
     * @return descriptive String
     */
    @Override
    public String toString() {
        return (alreadyDigipeated ? "already-digi'd" : "") + (when > 0 ? "@" + when : "");
    }
}
