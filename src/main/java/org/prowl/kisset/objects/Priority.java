package org.prowl.kisset.objects;

/**
 * Priorities ensure that time critical messages have a better chance of being
 * delivered quickly through the node network.  Packets with high priority are
 * permitted to jump any queues and be propagated sooner.
 */
public enum Priority {


    HIGH,    // things like chat
    MEDIUM,  // APRS and things that are not as important as chat
    LOW;     // background message sync


}
