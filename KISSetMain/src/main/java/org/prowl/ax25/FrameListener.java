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
 * This interface is implemented by any class that receives a message frame.
 * The caller is expected to identify the frame boundaries and specify the
 * start and length of the frame contents (but not the boundaries) for
 * consumption. Upon return, the frame is considered consumed and the buffer
 * containing it may be modified again.
 *
 * @author Andrew Pavlin, KA2DDO
 */
@FunctionalInterface
public interface FrameListener {
    /**
     * Consume and process one AX.25 frame containing some sort of message.
     *
     * @param frame the AX25Frame to be processed
     */
    void consumeFrame(AX25Frame frame);
}
