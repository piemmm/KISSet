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

/**
 * This class encapsulates one XID parameter. The caller is responsible for putting
 * the XIDParameter object into an appropriately typed XIDGroup object.
 */
public class XIDParameter {
    public static final byte[] EMPTY_VALUE = new byte[0];

    public byte paramIdentifier;
    public byte[] paramValue = null;

    // for use only by the reading code
    private XIDParameter() {
    }

    /**
     * Create an XIDParameter with an empty value (presence implies the value).
     *
     * @param paramIdentifier identifier code
     */
    public XIDParameter(byte paramIdentifier) {
        this.paramIdentifier = paramIdentifier;
        this.paramValue = EMPTY_VALUE;
    }

    /**
     * Create an XIDParameter with a 1-byte value.
     *
     * @param paramIdentifier identifier code
     * @param val             value of parameter
     */
    public XIDParameter(byte paramIdentifier, byte val) {
        this.paramIdentifier = paramIdentifier;
        this.paramValue = new byte[]{val};
    }

    /**
     * Create an XIDParameter with a 2-byte value.
     *
     * @param paramIdentifier identifier code
     * @param val             value of parameter
     */
    public XIDParameter(byte paramIdentifier, short val) {
        this.paramIdentifier = paramIdentifier;
        this.paramValue = new byte[2];
        this.paramValue[0] = (byte) (val >> 8);
        this.paramValue[1] = (byte) val;
    }

    /**
     * Create an XIDParameter with a 3- or 4-byte value.
     *
     * @param paramIdentifier identifier code
     * @param val             value of parameter
     * @param is3Byte         boolean true if value only requires 24 bits to encode
     */
    public XIDParameter(byte paramIdentifier, int val, boolean is3Byte) {
        this.paramIdentifier = paramIdentifier;
        this.paramValue = new byte[is3Byte ? 3 : 4];
        if (is3Byte) {
            this.paramValue[0] = (byte) (val >> 16);
            this.paramValue[1] = (byte) (val >> 8);
            this.paramValue[2] = (byte) val;
        } else {
            this.paramValue[0] = (byte) (val >> 24);
            this.paramValue[1] = (byte) (val >> 16);
            this.paramValue[2] = (byte) (val >> 8);
            this.paramValue[3] = (byte) val;
        }
    }

    /**
     * Read an XIDParameter from a byte stream.
     *
     * @param dis DataInput to read the parameter from
     * @return decoded XIDParameter element
     * @throws IOException if read fails for any reason
     */
    public static XIDParameter read(DataInput dis) throws IOException {
        XIDParameter p = new XIDParameter();
        p.paramIdentifier = dis.readByte();
        int len = dis.readUnsignedByte();
        if (len == 0) {
            p.paramValue = EMPTY_VALUE;
        } else {
            p.paramValue = new byte[len];
            dis.readFully(p.paramValue);
        }
        return p;
    }

    /**
     * Get the length of the parameter's value.
     *
     * @return length of value in octets (bytes)
     */
    public byte getParamLength() {
        if (paramValue.length > 255) {
            throw new IllegalArgumentException("paramValue cannot exceed 255 bytes in length, is " + paramValue.length + " bytes long now");
        }
        return (byte) paramValue.length;
    }

    /**
     * Write the XIDParameter to a byte stream.
     *
     * @param dos DataOutput to write the value to
     * @throws IOException if write fails for any reason
     */
    public void write(DataOutput dos) throws IOException {
        dos.writeByte(paramIdentifier);
        byte length = getParamLength();
        dos.writeByte(length);
        if (length != 0) {
            dos.write(paramValue);
        }
    }

    /**
     * Returns a string representation of the XIDParameter object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(paramValue.length * 3);
        b.append(Integer.toHexString((paramIdentifier & 0xFF) + 0x100).substring(1)).append(':');
        for (int i = 0; i < paramValue.length; i++) {
            b.append(Integer.toHexString((paramValue[i] & 0xFF) + 0x100).substring(1));
        }
        return b.toString();
    }
}
