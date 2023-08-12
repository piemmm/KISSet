package org.prowl.kisset.comms;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.kisset.objects.user.User;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class Service {

    private String name;

    public Service(String name) {
        this.name = name;
        if (name == null) {
            throw new AssertionError("Service name cannot be null");
        }
    }

    public abstract void start();

    public abstract void stop();

    public String getName() {
        return name;
    }

    /**
     * The callsign we are using for this service (used in the AX25 package so it knows what to respond to)
     * @return A calsign - might have an SSID, might not - who knows what the user decides!
     */
    public abstract String getCallsign();

    public abstract void acceptedConnection(User user, InputStream in, OutputStream out);

}
