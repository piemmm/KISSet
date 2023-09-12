package org.prowl.kisset.util;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.objects.user.User;

/**
 * This class takes %TOKENS% that are used in the message.properties file and detokenizes them with the values from
 * various sources.  For example, %PMSCALLSIGN% is replaced with the name of the BBS.
 */
public class UnTokenize {

    public static String str(String tokenizedStringToDetokenise) {

        // Global stuff
        Config config = KISSet.INSTANCE.getConfig();
        String pmsSSID = config.getConfig(Conf.pmsSSID, Conf.pmsSSID.stringDefault());
        String nodeSSID = config.getConfig(Conf.netromSSID, Conf.netromSSID.stringDefault());

        tokenizedStringToDetokenise = tokenizedStringToDetokenise.replace("%PMSCALLSIGN%", KISSet.INSTANCE.getMyCallNoSSID()+pmsSSID);
        tokenizedStringToDetokenise = tokenizedStringToDetokenise.replace("%NODECALLSIGN%", KISSet.INSTANCE.getMyCallNoSSID()+nodeSSID);

        return tokenizedStringToDetokenise;
    }

    public static String str(User user, String tokenizedStringToDetokenise) {
        tokenizedStringToDetokenise = str(tokenizedStringToDetokenise);

        // User stuff
        tokenizedStringToDetokenise = tokenizedStringToDetokenise.replace("%USERCALLSIGN%", user.getBaseCallsign());

        return tokenizedStringToDetokenise;
    }

}
