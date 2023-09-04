package org.prowl.ax25;


/**
 * Enum containing a list of KISS parameters
 */
public enum KissParameterType {
    TXDELAY(0x01),
    PERSISTENCE(0x02),
    SLOT_TIME(0x03),
    TX_TAIL(0x04),
    FULL_DUPLEX(0x05),
    SET_HARDWARE(0x06),
    RETURN(0xFF);
    private final int value;

    KissParameterType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}