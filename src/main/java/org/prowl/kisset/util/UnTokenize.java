package org.prowl.kisset.util;

import org.prowl.kisset.KISSet;

/**
 * This class takes %TOKENS% that are used in the message.properties file and detokenizes them with the values from
 * various sources.  For example, %PMSNAME% is replaced with the name of the BBS.
 */
public class UnTokenize {

    public static String str(String tokenizedStringToDetokenise) {

        tokenizedStringToDetokenise = tokenizedStringToDetokenise.replace("%PMSCALLSIGN%", KISSet.INSTANCE.getMyCallNoSSID());

        return tokenizedStringToDetokenise;
    }


}
