package org.prowl.kisset.config;

import org.prowl.kisset.KISSet;

/**
 * List of configuration names used in the xml file.
 */
public enum Conf {

    // Enum list of configuration variables with their defaults
    callsign(""),
    monitor(false),
    terminalFont(OSDefaults.getDefaultPlatformFont()),
    terminalFontSize(14),
    monitorTransparency(0),
    terminalTransparency(0),
    dxTransparency(0),
    fbbTransparency(0),


    // These settings are set per-interface
    uuid(""),
    beaconEvery(0),
    beaconText(""),

    // When someone connects to you
    connectGreetingText("Hi! This is a KISSet application - If there is no reply it might be because I'm not watching the screen!"),

    // Mailbox service
    pmsEnabled(true),
    pmsSSID("-2"),
    pmsGreetingText("Hi! Welcome to the KISSet Mailbox - type 'help' for a list of commands!"),

    // Net/ROM service
    netromEnabled(true),
    netromSSID("-1"),
    netromAlias(Conf.createDefaultNetromAlias()), // A blank alias, with enabled, will default to the last 3 letters of call+'NOD'
    netromGreetingText("Hi! This is a KISSet Net/ROM node - type 'help' for a list of commands!"),

    // Remote access - allows the user to connect, and access to the Mailbox system securely over a LAN or internet.
    sshPort(0),
    sshUsername(""),
    sshPassword(""),

    // FBB compatible client system
    fbbListeningActive(true),
    fbbPreferredBBSCallsign(""),

    // APRS settings
    aprsDecoingOverKISSEnabled(true),
    connectToAPRSIServer(true),
    aprsIServerHostname("aprs-cache.g0tai.net:14580"),

    // MQTT settings
    mqttPacketUploadEnabled(false),
    mqttBrokerHostname(""),
    mqttBrokerUsername(""),
    mqttBrokerPassword(""),
    mqttBrokerTopic("");

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

    /**
     * Create a default Net/ROM alias based on the callsign.
     * @return
     */
    public static final String createDefaultNetromAlias() {
        if (KISSet.INSTANCE.getMyCall().length() > 0) {
            return KISSet.INSTANCE.getMyCallNoSSID().substring(KISSet.INSTANCE.getMyCallNoSSID().length() - 3) + "NOD";
        } else {
            return "";
        }


    }

}
