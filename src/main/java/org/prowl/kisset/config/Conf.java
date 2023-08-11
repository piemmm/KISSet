package org.prowl.kisset.config;

/**
 * List of configuration names used in the xml file.
 */
public enum Conf {

    // Enum list of configuration variables with their defaults
    callsign(""),
    monitor(false),
    terminalFont("Monospaced"),
    terminalFontSize(12),
    monitorTransparency(0),
    terminalTransparency(0),

    // These settings are set per-interface
    uuid(""),
    beaconEvery(0),
    beaconText(""),

    // When someone connects to you
    connectGreetingText(""),

    // Mailbox system
    pmsActive(false),
    pmsSSID(""),
    pmsGreetingText(""),

    // Remote access - allows the user to connect, and access to the Mailbox system
    sshPort(0),
    sshUsername(""),
    sshPassword(""),

    // FBB compatible client system
    fbbListeningActive(true),
    fbbPreferredBBSCallsign("");






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
