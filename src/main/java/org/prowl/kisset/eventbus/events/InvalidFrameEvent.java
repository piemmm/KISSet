package org.prowl.kisset.eventbus.events;

import org.prowl.kisset.ax25.BasicTransmittingConnector;

/**
 * Represents an invalid AX.25/KISS frame being received.
 */
public class InvalidFrameEvent extends BaseEvent {

    /**
     * The invalid data.
     */
    public final byte[] invalidData;

    /**
     * The connector that received the invalid data.
     */
    public final BasicTransmittingConnector connector;

    public InvalidFrameEvent(byte[] invalidData, BasicTransmittingConnector connector) {
        this.invalidData = invalidData;
        this.connector = connector;
    }
}
