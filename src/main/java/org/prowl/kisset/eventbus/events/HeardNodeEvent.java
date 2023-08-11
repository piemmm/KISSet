package org.prowl.kisset.eventbus.events;


import org.prowl.kisset.core.Node;
import org.prowl.kisset.util.Tools;

public class HeardNodeEvent extends BaseEvent {

    private final Node node;

    public HeardNodeEvent(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public boolean isValidPacket() {
        return node.getFrame().isValid() && Tools.isAlphaNumeric(node.getCallsign());
    }

}
