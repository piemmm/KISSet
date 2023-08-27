package org.prowl.kisset.eventbus.events;


import net.ab0oo.aprs.parser.APRSPacket;

public class APRSPacketEvent extends BaseEvent {

    private final APRSPacket aprsPacket;

    public APRSPacketEvent(APRSPacket aprsPacket) {
        this.aprsPacket = aprsPacket;

    }

    public APRSPacket getAprsPacket() {
        return aprsPacket;
    }


}

