package org.prowl.kisset.config;

/**
 * A fixed list of sensible beacon intervals for the user to choose from.
 */
public enum BeaconType {

    INTERVAL0("No beacon", 0),
    INTERVAL5("5 minute intervals", 5),
    INTERVAL10("10 minute intervals", 10),
    INTERVAL15("15 minute intervals", 15),
    INTERVAL30("30 minute intervals", 30),
    INTERVAL60("60 minute intervals", 60),
    INTERVAL120("120 minute intervals", 120),
    INTERVAL240("240 minute intervals", 240);

    private final String description;
    private final int interval;

    BeaconType(String description, int interval) {
        this.description = description;
        this.interval = interval;
    }

    public static BeaconType getBeaconType(int interval) {
        for (BeaconType beaconType : BeaconType.values()) {
            if (beaconType.getInterval() == interval) {
                return beaconType;
            }
        }
        return BeaconType.INTERVAL0;
    }

    public String getDescription() {
        return description;
    }

    public int getInterval() {
        return interval;
    }

    public String toString() {
        return description;
    }
}
