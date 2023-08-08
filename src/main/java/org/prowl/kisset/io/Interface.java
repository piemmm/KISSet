package org.prowl.kisset.io;

import org.prowl.kisset.ax25.ConnectionEstablishmentListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class Interface {

    // If the interface fails to start, the reason will be logged here.
    String failReason;
    private int currentStream = 0;
    private List<Stream> streams = new ArrayList<>();

    public Interface() {

        // Create default streams.
        for (int i = 0; i < 7; i++) {
            streams.add(new Stream(this));
        }
    }

    public abstract void start() throws IOException;

    public abstract void stop();

    public abstract String getUUID();

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
}
