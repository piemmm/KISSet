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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;

/**
 * This class encapsulates and encodes one X.25 XID group. This is used to handle
 * responding to the XID frame in the AX.25 protocol.
 */
public class XIDGroup {
    public final ArrayList<XIDParameter> paramList = new ArrayList<XIDParameter>();
    public byte formatIdentifier;
    public byte groupIdentifier;

    /**
     * Create an empty XIDGroup with the AX.25 default FI/GI.
     */
    public XIDGroup() {
        formatIdentifier = (byte) 0x82;
        groupIdentifier = (byte) 0x80;
    }

    /**
     * Read an XIDGroup from an input byte stream.
     *
     * @param dis DataInput to read the XIDGroup from
     * @return decoded XIDGroup
     * @throws IOException if read fails for any reason
     */
    public static XIDGroup read(DataInput dis) throws IOException {
        XIDGroup g = new XIDGroup();
        g.formatIdentifier = dis.readByte();
        g.groupIdentifier = dis.readByte();
        int len = dis.readUnsignedShort();
        while (len > 0) {
            XIDParameter p = XIDParameter.read(dis);
            len -= 2 + p.getParamLength();
        }
        if (len < 0) {
            throw new ProtocolException("groupLength didn't contain whole set of XIDParameters");
        }
        return g;
    }

    /**
     * Write the XIDGroup to a byte stream.
     *
     * @param dos DataOutput to write the XIDGroup to
     * @throws IOException if write fails for any reason
     */
    public void write(DataOutput dos) throws IOException {
        dos.writeByte(formatIdentifier);
        dos.writeByte(groupIdentifier);
        dos.writeShort(getGroupLength());
        for (XIDParameter p : paramList) {
            p.write(dos);
        }
    }

    /**
     * Get the number of bytes needed to encode the list of XIDParameters in this XIDGroup.
     *
     * @return byte count
     */
    public int getGroupLength() {
        int len = 0;
        for (XIDParameter p : paramList) {
            len += p.getParamLength() + 2;
        }
        return len;
    }
}
