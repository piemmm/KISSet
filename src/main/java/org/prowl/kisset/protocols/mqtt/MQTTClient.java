package org.prowl.kisset.protocols.mqtt;

import com.google.common.eventbus.Subscribe;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;

/**
 * This is responsible for listening to packets on all the interfaces, then forwarding them to an MQTT broker
 */
public class MQTTClient {

    public MQTTClient() {

        // Register this client with the event bus so we can get packets from all the interfaces.
        SingleThreadBus.INSTANCE.register(this);
    }


    /**
     * This is called when a packet is received on an interface
     * @param event
     */
    @Subscribe
    public void onReceivedPacketEvent(HeardNodeEvent event) {

    }
}
