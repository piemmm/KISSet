package org.prowl.kisset.objects.routing;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.io.Interface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * This represents the contents of a routing packet sent using the INP3 protocol
 */
public class INP3Route {

    private static final long MAX_AGE = 1000L * 60L * 60L * 2; // 2 hours for we drop the route as a hard limit.
    private String sourceCallsign;
    private Interface anInterface;

    private String destinationNodeCallsign;
    private int hops;
    private long tripTime;
    private List<INP3Option> options = new ArrayList<>();

    private long lastHeard;

    public INP3Route() {

    }

    public INP3Route(Interface anInterface, String sourceCallsign, String destinationNodeCallsign, int hops, long tripTime, List<INP3Option> options) {
        this.anInterface = anInterface;
        this.sourceCallsign = sourceCallsign;
        this.destinationNodeCallsign = destinationNodeCallsign;
        this.hops = hops;
        this.tripTime = tripTime;
        this.options = options;
        this.lastHeard = System.currentTimeMillis();
    }

    public void addOption(INP3Option option) {
        options.add(option);
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - lastHeard) > MAX_AGE;
    }

    public Interface getAnInterface() {
        return anInterface;
    }

    public String getSourceCallsign() {
        return sourceCallsign;
    }

    public String getDestinationNodeCallsign() {
        return destinationNodeCallsign;
    }

    public int getHops() {
        return hops;
    }

    public long getTripTime() {
        return tripTime;
    }

    public List<INP3Option> getOptions() {
        return options;
    }

    public long getLastHeard() {
        return lastHeard;
    }

    public boolean hasAlias() {
        return getAlias() != null;
    }

    public String getAlias() {
        for (INP3Option option : options) {
            if (option.type == INP3OptionType.ALIAS) {
                return new String(option.data);
            }
        }
        return null;
    }

    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        StringBuilder builder = new StringBuilder();
        int interfaceNumber = KISSet.INSTANCE.getInterfaceHandler().getInterfaceNumber(anInterface);
        builder.append(interfaceNumber + ": ");
        builder.append(sourceCallsign);
        builder.append(" advertises");
        builder.append(" " + destinationNodeCallsign);
        if (hasAlias()) {
            builder.append("/" + getAlias());
        }
        builder.append(" hops ");
        builder.append(hops);
        builder.append(", trip time ");
        builder.append(tripTime);
        builder.append(", heard at " + sdf.format(lastHeard));
        return builder.toString();
    }

    /**
     * Represents the option type in an INP3 option
     */
    public enum INP3OptionType {
        ALIAS(0),
        IP(1),
        APRS_POSITION(0x10), // 4 byes lat, 4 bytes lon, in degrees * 6000
        NODETYPE(0x11),
        TIMESTAMP(0x12), // 4 bytes, secs since 1970, inserted by originator, must *never* be changed
        TCP_SERVICE(0x13), // tcp service number for telnet via amprnet, 2 bytes in network byte order
        TZOFFSET(0x14), // Timezone offset from GMT
        MAIDENHEAD(0x15), // Maidenhead locator. String, no null terminator
        QTH(0x16), // QTH locator, String, no null terminator
        SOFTWARE_VERSION(0x17);

        private final int value;

        INP3OptionType(int value) {
            this.value = value;
        }

        public static INP3OptionType fromValue(int value) {
            for (INP3OptionType type : INP3OptionType.values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            return null;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * A packet can contain a list of these options
     */
    public static class INP3Option {

        INP3OptionType type;
        byte[] data;

        public INP3Option(INP3OptionType type, byte[] data) {

            // BUGBUG workaround for XRouter incorrectly appends a CRLF to the destinationcallsign in
            //        INP3 packets. - we need to log this incorrectness and strip it.
            if (INP3OptionType.ALIAS.equals(type)) {
                String alias = new String(data);
                if (alias.contains("\r") || alias.contains("\n")) {
                    alias = alias.replaceAll("\r", "").replaceAll("\n", "");
                    data = alias.getBytes();
                }
            }

            this.type = type;
            this.data = data;
        }
    }
}
