package org.prowl.kisset.protocols.mqtt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.prowl.kisset.protocols.core.Node;

import java.util.concurrent.Callable;

public class MQTTPacket implements Callable<Void> {

    private static final Log LOG = LogFactory.getLog("MQTTPacket");


    private final IMqttClient client;
    private final String topic;
    private final Node node;

    public MQTTPacket(IMqttClient client, Node node, String topic) {
        this.client = client;
        this.topic = topic;
        this.node = node;
    }

    @Override
    public Void call() throws Exception {
        if (!client.isConnected()) {
            return null;
        }
        MqttMessage msg = readPacket();
        msg.setQos(0);
        msg.setRetained(true);
        client.publish(topic, msg);
        return null;

    }

    private MqttMessage readPacket() {
        return new MqttMessage(node.getFrame().getRawPacket());
    }
}
