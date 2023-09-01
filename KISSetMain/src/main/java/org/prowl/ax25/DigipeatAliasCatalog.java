package org.prowl.ax25;
/*
 * Copyright (C) 2011-2022 Andrew Pavlin, KA2DDO
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 * This class manages the list of digipeat aliases recognized by YAAC.
 */
public class DigipeatAliasCatalog implements Iterable<DigipeatAliasRecord> {
    private static final DigipeatAliasRecord[] DEFAULT_DIGIPEAT_ALIASES = {
            new DigipeatAliasRecord("WIDE1", "true,false,true"),
            new DigipeatAliasRecord("WIDE2", "true,false,true"),
            new DigipeatAliasRecord("TEMP1", "true,true,false")
    };
    private static final DigipeatAliasCatalog instance = new DigipeatAliasCatalog();
    private final ArrayList<DigipeatAliasRecord> aliasList = new ArrayList<DigipeatAliasRecord>();

    private DigipeatAliasCatalog() {
    }

    /**
     * Get a reference to the singleton DigipeatAliasCatalog.
     *
     * @return the DigipeatAliasCatalog object
     */
    public static DigipeatAliasCatalog getInstance() {
        return instance;
    }

    /**
     * Load the catalog with the factory defaults.
     *
     * @param aliasNode Preferences node in which to store the catalog entries
     */
    public static void loadDefaults(Preferences aliasNode) {
        for (DigipeatAliasRecord dar : DigipeatAliasCatalog.DEFAULT_DIGIPEAT_ALIASES) {
            DigipeatAliasCatalog.getInstance().addRow(dar);
            dar.writeToPreferences(aliasNode);
        }
    }

    /**
     * Test if this callsign looks like a digipeat New-N alias.
     *
     * @param relay digipeater AX25Callsign to test
     * @return boolean true if it looks like a New-N alias or other known alias
     */
    public static boolean isRelayAStep(AX25Callsign relay) {
        ArrayList<DigipeatAliasRecord> aliasList1 = instance.aliasList; // avoid getfield opcode
        for (int aliasIdx = aliasList1.size() - 1; aliasIdx >= 0; aliasIdx--) {
            DigipeatAliasRecord dar = aliasList1.get(aliasIdx);
            String baseCallsign = relay.getBaseCallsign();
            if (dar.isN_N) {
                int rLen = baseCallsign.length();
                int aLen = dar.alias.length();
                if (rLen == aLen &&
                        baseCallsign.startsWith(dar.alias)) {
                    char ch1 = baseCallsign.charAt(aLen - 1);
                    if (ch1 > '0' && ch1 <= '7') {
                        return true;
                    }
                }
            } else if (baseCallsign.regionMatches(0, dar.alias, 0, baseCallsign.length())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the number of defined digipeat aliases.
     *
     * @return alias count
     */
    public int getRowCount() {
        return aliasList.size();
    }

    /**
     * Add an empty digipeat alias definition to the catalog (used for user data entry of a new alias).
     */
    public void addRow() {
        DigipeatAliasRecord dar = new DigipeatAliasRecord();
        aliasList.add(dar);
    }

    /**
     * Add a pre-filled-in digipeat alias to the catalog,
     *
     * @param dar DigipeatAliasRecord to add
     * @throws IllegalArgumentException if alias already exists in catalog
     */
    public void addRow(DigipeatAliasRecord dar) {
        if (aliasList.contains(dar)) {
            throw new IllegalArgumentException("catalog already contains alias " + dar.alias);
        }
        aliasList.add(dar);
    }

    /**
     * Deletes the specified alias row from the catalog.
     *
     * @param rowIndex zero-based index of alias to delete
     */
    public void deleteRow(int rowIndex) {
        aliasList.remove(rowIndex);
    }

    /**
     * Get the Nth DigipeatAliasRecord in the catalog.
     *
     * @param rowIndex zero-based row index
     * @return DigipeatAliasRecord
     */
    public DigipeatAliasRecord getRow(int rowIndex) {
        return aliasList.get(rowIndex);
    }

    /**
     * Get a Digipeat alias record corresponding to the specified digipeater callsign, if
     * such a record exists.
     *
     * @param baseCallsign String callsign to search for
     * @return DigipeatAliasRecord describing the authorized alias, or null if no enabled match
     */
    public DigipeatAliasRecord getDigipeatRecord(String baseCallsign) {
        for (DigipeatAliasRecord dar : aliasList) {
            if (dar.alias.equalsIgnoreCase(baseCallsign)) {
                return dar;
            }
        }
        return null;
    }

    /**
     * Get a Digipeat alias record corresponding to the specified digipeater callsign, if
     * such a record exists and is enabled.
     *
     * @param callsign String callsign to search for
     * @return DigipeatAliasRecord describing the authorized alias, or null if no enabled match
     */
    public DigipeatAliasRecord getEnabledDigipeatRecord(AX25Callsign callsign) {
        for (DigipeatAliasRecord dar : aliasList) {
            String baseCallsign = callsign.getBaseCallsign();
            if (dar.enabled &&
                    (dar.isN_N
                            ? baseCallsign.startsWith(dar.alias) && callsign.getSSID() > 0
                            : (callsign.getSSID() == 0 ? dar.alias.equalsIgnoreCase(baseCallsign) :
                            dar.alias.equalsIgnoreCase(callsign.toString())))) {
                return dar;
            }
        }
        return null;
    }

    /**
     * Returns an iterator over the catalog of DigipeatAliasRecords.
     *
     * @return an Iterator.
     */
    public Iterator<DigipeatAliasRecord> iterator() {
        return aliasList.iterator();
    }

    /**
     * Identify what appears to be a regional alias base in the list of known aliases,
     * if one exists.
     *
     * @return String region code name, or null if no region code defined
     */
    public String getRegionCode() {
        for (DigipeatAliasRecord dar : aliasList) {
            if (dar.isN_N) {
                boolean isDefault = false;
                for (DigipeatAliasRecord defaultDAR : DEFAULT_DIGIPEAT_ALIASES) {
                    if (dar.alias.equals(defaultDAR.alias)) {
                        isDefault = true;
                        break;
                    }
                }
                if (!isDefault && dar.enabled &&
                        dar.alias.length() > 2 && Character.isDigit(dar.alias.charAt(dar.alias.length() - 1))) {
                    return dar.alias.substring(0, dar.alias.length() - 1);
                }
            }
        }
        return null;
    }
}
