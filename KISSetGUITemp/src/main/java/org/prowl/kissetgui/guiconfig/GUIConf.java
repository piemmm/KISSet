package org.prowl.kissetgui.guiconfig;

import org.prowl.kisset.KISSet;

public enum GUIConf {

    terminalFont(OSDefaults.getDefaultPlatformFont());


    public Object defaultSetting;

    GUIConf(Object defaultSetting) {
        this.defaultSetting = defaultSetting;
    }

    /**
     * Create a default Net/ROM alias based on the callsign.
     *
     * @return
     */
    public static final String createDefaultNetromAlias() {
        if (KISSet.INSTANCE.getMyCall().length() > 0) {
            return KISSet.INSTANCE.getMyCallNoSSID().substring(KISSet.INSTANCE.getMyCallNoSSID().length() - 3) + "NOD";
        } else {
            return "";
        }


    }

    public String stringDefault() {
        return String.valueOf(defaultSetting);
    }

    public int intDefault() {
        return Integer.parseInt(String.valueOf(defaultSetting));
    }

    public boolean boolDefault() {
        return Boolean.parseBoolean(String.valueOf(defaultSetting));
    }

}
