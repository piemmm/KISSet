package org.prowl.kisset.io;

import org.prowl.kisset.ax25.ConnectionEstablishmentListener;

import java.io.IOException;

public abstract class Interface {


    public abstract void start() throws IOException;

    public abstract void stop();

    public abstract String getUUID();

    public abstract boolean connect(String to, String from, ConnectionEstablishmentListener connectionEstablishmentListener) throws IOException;

}
