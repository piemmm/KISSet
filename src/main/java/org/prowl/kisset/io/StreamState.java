package org.prowl.kisset.io;

/**
 * A stream state represents the state of the connection on a paticular interface (or stream on that interface)
 * at a given time.
 */
public enum StreamState {

    // Connected to a remote station and are conversing with it
    CONNECTED,

    // We are disconnected from the remote station and are not connecting.
    DISCONNECTED,

    // We are trying to connect to a remote station.
    CONNECTING,

    // In a connected state, but we are requesting a graceful disconnect
    DISCONNECTING;


}
