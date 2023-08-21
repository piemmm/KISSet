package org.prowl.kisset.objects.routing;

import org.prowl.kisset.KISSet;
import org.prowl.kisset.io.Interface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class INP3Route {

    // INP3 is time based, and this is the hard maximum age we will keep a 'non-responding' node for.
    private static final long MAX_AGE = 1000 * 60 * 60 * 2; // 2 hours

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

    /**
     * A packet can contain a list of these options
     */
    public static class INP3Option {

        INP3OptionType type;
        byte[] data;

        public INP3Option(INP3OptionType type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }

    /**
     * Represents the option type in an INP3 option
     */
    public enum INP3OptionType {
        ALIAS(0),
        IP(1);

        private int value;

        private INP3OptionType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static INP3OptionType fromValue(int value) {
            for (INP3OptionType type : INP3OptionType.values()) {
                if (type.getValue() == value) {
                    return type;
                }
            }
            return null;
        }
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
}
