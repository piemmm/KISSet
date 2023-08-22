package org.prowl.kisset.protocols.mqtt;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.util.Tools;

import java.util.UUID;

/**
 * This is responsible for listening to packets on all the interfaces, then forwarding them to an MQTT broker
 */
public class MQTTClient {

    private static final Log LOG = LogFactory.getLog("MQTTClient");


    private boolean running = false;

    private String topic;
    private String broker;
    private String username;
    private String password;
    private IMqttClient publisher;
    private Config config;

    public MQTTClient() {
        // Get configuration
        config = KISSet.INSTANCE.getConfig();

        broker = config.getConfig(Conf.mqttBrokerHostname, Conf.mqttBrokerHostname.stringDefault());
        username =config.getConfig(Conf.mqttBrokerUsername, Conf.mqttBrokerUsername.stringDefault());
        password = config.getConfig(Conf.mqttBrokerPassword, Conf.mqttBrokerPassword.stringDefault());
        topic = config.getConfig(Conf.mqttBrokerTopic, Conf.mqttBrokerTopic.stringDefault());
    }

    public void start() {
        running = true;

        // Register this client with the event bus so we can get packets from all the interfaces.
        SingleThreadBus.INSTANCE.register(this);

        Tools.runOnThread(() -> {
            boolean isEnabled = config.getConfig(Conf.mqttPacketUploadEnabled, Conf.mqttPacketUploadEnabled.boolDefault());
            while (running && isEnabled) {
                Tools.delay(1000);

                try {
                    LOG.info("Starting MQTT client");
                    String publisherId = UUID.randomUUID().toString();
                    publisher = new MqttClient(broker, publisherId);

                    MqttConnectOptions options = new MqttConnectOptions();
                    options.setAutomaticReconnect(true);
                    options.setCleanSession(true);
                    options.setUserName(username);
                    options.setPassword(password.toCharArray());
                    options.setConnectionTimeout(10);
                    publisher.connect(options);

                    LOG.info("Connected to MQTT broker " + broker);

                    // Subscribe to the topic
                    publisher.subscribe(topic);

                } catch (MqttException e) {
                    LOG.error(e.getMessage(), e);
                }

                // Wait here so we can loop easily if something bad happens to the connection or the broker goes away
                while (publisher != null && publisher.isConnected()) {
                    Tools.delay(5000);
                }

                isEnabled = config.getConfig(Conf.mqttPacketUploadEnabled, Conf.mqttPacketUploadEnabled.boolDefault());
            }

        });

    }

    @Subscribe
    public void configurationChanged(ConfigurationChangedEvent event) {
        LOG.info("Configuration changed, reconnecting to MQTT broker");
        if (publisher != null) {
            try {
                // Force a reconnect
                publisher.disconnect();
            } catch (MqttException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public void stop() {
        SingleThreadBus.INSTANCE.unregister(this);
        running = false;

    }


    /**
     * This is called when a packet is received on an interface
     *
     * @param event
     */
    @Subscribe
    public void onReceivedPacketEvent(HeardNodeEvent event) {
        try {
            if (publisher != null && publisher.isConnected()) {
                LOG.debug("Sending packet to MQTT broker");
                publisher.publish(topic, event.getNode().getFrame().getRawPacket(), 0, true);
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
