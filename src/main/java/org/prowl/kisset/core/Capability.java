package org.prowl.kisset.core;

/**
 * Reference to what services this node is running and when that service was last seen so it can be expired;
 */
public class Capability {
    private Node.Service service;
    private long lastSeen;

    public Capability(Node.Service service, long lastSeen) {
        this.service = service;
        this.lastSeen = lastSeen;
    }

    public Capability(Node.Service service) {
        this.service = service;
        this.lastSeen = System.currentTimeMillis();
    }

    public Node.Service getService() {
        return service;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}