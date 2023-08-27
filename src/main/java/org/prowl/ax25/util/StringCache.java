package org.prowl.ax25.util;
/*
 * Copyright (C) 2011-2012 Andrew Pavlin, KA2DDO
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
 * This singleton class provides a pruneable cache for constant Strings that won't
 * eat up the PermGen part of the Java JVM heap.
 */
public final class StringCache extends ShareableObjectCache<String> {
    private static final StringCache instance = new StringCache();

    private StringCache() {
    }

    public static String intern(final String s) {
        return instance.internKey(s);
    }

    public static String paramString() {
        return instance.toString();
    }

    protected Class getType() {
        return String.class;
    }
}
