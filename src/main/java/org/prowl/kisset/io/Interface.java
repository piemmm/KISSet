package org.prowl.kisset.io;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.kisset.ax25.ConnectionEstablishmentListener;
import org.prowl.kisset.config.Conf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Interface {

    // If the interface fails to start, the reason will be logged here.
    String failReason;
    private int currentStream = 0;
    private List<Stream> streams = new ArrayList<>();
    protected HierarchicalConfiguration config;
    private String uuid;

    public Interface(HierarchicalConfiguration config) {
        this.config = config;
        getUUID();

        // Create default streams.
        for (int i = 0; i < 7; i++) {
            streams.add(new Stream(this));
        }
    }

    public abstract void start() throws IOException;

    public abstract void stop();

    public abstract boolean connect(String to, String from, ConnectionEstablishmentListener connectionEstablishmentListener) throws IOException;

    public abstract void disconnect(Stream currentStream);

    public abstract void cancelConnection(Stream stream);

    public Stream getStream(int stream) {
        return streams.get(stream);
    }

    public Stream getCurrentStream() {
        return streams.get(currentStream);
    }

    public void setCurrentStream(int currentStream) {
        this.currentStream = currentStream;
    }

    public List<Stream> getStreams() {
        return new ArrayList<>(streams);
    }

    public String getFailReason() {
        return failReason;
    }


    public String getUUID() {
        uuid = config.getString(Conf.uuid.name());
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
        config.setProperty(Conf.uuid.name(), uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interface that = (Interface) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
