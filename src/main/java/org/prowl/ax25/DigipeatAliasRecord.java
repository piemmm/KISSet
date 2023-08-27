package org.prowl.ax25;
/*
 * Copyright (C) 2011-2018 Andrew Pavlin, KA2DDO
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

import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * This class defines all the parameters for an alias used for digipeating.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public class DigipeatAliasRecord implements Comparable<DigipeatAliasRecord> {
    /**
     * The name of the alias, such as WIDE2. Should be an uppercase String.
     */
    public String alias = "";
    /**
     * Specify if this is an alias using the <a href="http://aprs.org/fix14439.html">New n-N paradigm</a>.
     */
    public boolean isN_N;
    /**
     * Specify if this digipeat alias is globally enabled for digipeating.
     */
    public boolean enabled;
    /**
     * Specify whether digipeats using this alias should have trace callsigns inserted into the digipeat path
     * of the AX.25 frame.
     */
    public boolean isTraced;

    /**
     * Create an empty DigipeatAliasRecord.
     */
    public DigipeatAliasRecord() {
    }

    /**
     * Create a DigipeatAliasRecord using string format attributes.
     *
     * @param alias  String name of alias
     * @param params String of comma-separated boolean strings, indicating whether
     *               this is a n-N alias, the alias is enabled, and whether this alias should be traced
     */
    public DigipeatAliasRecord(String alias, String params) {
        this.alias = alias;
        parseParamsString(params);
    }

    private void parseParamsString(String params) {
        StringTokenizer tk = new StringTokenizer(params, ",");
        isN_N = Boolean.parseBoolean(tk.nextToken());
        enabled = Boolean.parseBoolean(tk.nextToken());
        isTraced = Boolean.parseBoolean(tk.nextToken());
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     * argument; <code>false</code> otherwise.
     * @see #hashCode()
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DigipeatAliasRecord) {
            return alias.equals(((DigipeatAliasRecord) obj).alias);
        }
        return false;
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.org.ka2ddo.util.Hashtable</code>.
     *
     * @return a hash code value for this object.
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return alias.hashCode();
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "DigipeatAliasRecord[" + getAliasString() + ']';
    }

    /**
     * Create a complete alias string, accounting for aliases using the New n-N paradigm.
     *
     * @return complete alias String
     */
    public String getAliasString() {
        return alias + (isN_N ? "-" + alias.substring(alias.length() - 1) : "");
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this object.
     */
    public int compareTo(DigipeatAliasRecord o) {
        return alias.compareTo(o.alias);
    }

    /**
     * Store this DigipeatAliasRecord in Java Preferences
     *
     * @param prefs the Preferences node to contain the saved record data
     */
    public void writeToPreferences(Preferences prefs) {
        prefs.put(alias, String.valueOf(isN_N) + ',' + enabled + ',' + isTraced);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Fill this record from data saved under the alias name in Java Preferences.
     *
     * @param prefs Preferences node containing the saved data
     */
    public void readFromPreferences(Preferences prefs) {
        parseParamsString(prefs.get(alias, "false,false,false"));
    }
}
