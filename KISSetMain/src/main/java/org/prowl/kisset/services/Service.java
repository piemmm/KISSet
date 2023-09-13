package org.prowl.kisset.services;

import org.prowl.ax25.AX25Frame;
import org.prowl.ax25.AX25InputStream;
import org.prowl.ax25.AX25OutputStream;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.user.User;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class Service {

    private final String name;

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
     *
     * @return A calsign - might have an SSID, might not - who knows what the user decides!
     */
    public abstract String getCallsign();

    /**
     * Called when a connection is accepted.
     *
     * @param anInterface The interface the connection was accepted on - note not to be used to control connection states. Connectionless only as the
     *                    interface may only reflect the net/rom connection and not actually have a physical connection to the remote node as it is
     *                    net/rom tunneled.
     * @param user        The user that is connecting
     * @param in          The input stream for the connection
     * @param out         The output stream for the connection - close this to end the connection.
     */
    public abstract void acceptedConnection(Interface anInterface, User user, InputStream in, OutputStream out);


    /**
     * The frame type used for I frames
     * @return
     */
    public byte getServicePid(User user) {
        return AX25Frame.PID_NOLVL3;
    }
}
