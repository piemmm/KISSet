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
 * This enumeration categorizes AX.25 messages by what protocol they are carrying,
 * to make it easier for YAAC I/O ports to determine if a particular AX25Frame should
 * be sent out a particular port.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public enum ProtocolFamily {
    /**
     * APRS packets over AX.25 UI frames, PID=0xF0 (NOLVL3).
     */
    APRS(AX25Frame.PID_NOLVL3),
    /**
     * OpenTRAC packets over AX.25 UI frames, PID=0x77 (OPENTRAC).
     */
    OPENTRAC(AX25Frame.PID_OPENTRAC),
    /**
     * Other level 2 AX.25 frames, including connected-mode frames, PID=0xF0 (NOLVL3).
     */
    RAW_AX25(AX25Frame.PID_NOLVL3);

    private final byte pid;

    ProtocolFamily(byte pid) {
        this.pid = pid;
    }

    /**
     * Get the AX.25 protocol ID value associated with this ProtocolFamily.
     *
     * @return PID value
     */
    public byte getPid() {
        return pid;
    }
}
