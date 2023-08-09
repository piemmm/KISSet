package org.prowl.kisset.config;

/**
 * List of configuration names used in the xml file.
 */
public enum Conf {

    callsign(""),
    monitor(false),
    uuid(""),
    terminalFont("Monospaced"),
    terminalFontSize(12);

    public Object defaultSetting;

    Conf(Object defaultSetting) {
        this.defaultSetting = defaultSetting;
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
