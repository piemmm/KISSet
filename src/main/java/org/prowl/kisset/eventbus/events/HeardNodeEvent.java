package org.prowl.kisset.eventbus.events;


import org.prowl.kisset.core.Node;

public class HeardNodeEvent extends BaseEvent {

    private final Node node;

    public HeardNodeEvent(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

}
