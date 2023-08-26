package org.prowl.kisset.io;

import org.prowl.kisset.comms.host.parser.ExtensionState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Stream {

    private StreamState streamState;

    private volatile InputStream in;

    private volatile OutputStream out;

    private ExtensionState extensionState = ExtensionState.NONE;

    private String remoteCall;

    private Interface anInterface;

    public Stream(Interface anInterface) {
        this.anInterface = anInterface;
        streamState = StreamState.DISCONNECTED;
    }

    public StreamState getStreamState() {
        return streamState;
    }

    public void setStreamState(StreamState streamState) {
        if (streamState.equals(StreamState.DISCONNECTED)) {
            remoteCall = null;
        }
        this.streamState = streamState;
    }

    public ExtensionState getExtensionState() {
        return extensionState;
    }

    public void setExtensionState(ExtensionState extensionState) {
        this.extensionState = extensionState;
    }

    public void setIOStreams(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    // Convenience method to write to the stream
    public void write(byte[] data) throws IOException {
        out.write(data);
    }

    // Convenience method to flush the stream
    public void flush() throws IOException {
        out.flush();
    }

    // Send a disconnect frame and the wait for the ax.25 stack to disconnect
    public void disconnect() throws IOException {
        anInterface.disconnect(this);
        in.close();
        out.close();
        streamState = StreamState.DISCONNECTING;
    }

    // Disconnect immediately without sending a disconnect request frame.
    public void disconnectNow() throws IOException {
        in.close();
        out.close();
        streamState = StreamState.DISCONNECTED;
    }

    public String getRemoteCall() {
        return remoteCall;
    }

    public void setRemoteCall(String remoteCall) {
        this.remoteCall = remoteCall;
    }
}
