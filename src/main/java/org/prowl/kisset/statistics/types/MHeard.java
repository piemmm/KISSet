package org.prowl.kisset.statistics.types;

import com.google.common.eventbus.Subscribe;
import org.prowl.kisset.protocols.core.Capability;
import org.prowl.kisset.protocols.core.Node;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.util.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MHeard {

    protected final List<Node> heardList;

    public MHeard() {
        heardList = Collections.synchronizedList(new LinkedList<Node>());
        SingleThreadBus.INSTANCE.register(this);
    }

    public List<Node> listHeard() {
        synchronized (heardList) {
            return new ArrayList<Node>(heardList);
        }
    }

    public void addToFront(Node heard) {
        synchronized (heardList) {
            int index = heardList.indexOf(heard);
            if (index != -1) {
                // Update existing node
                Node oldHeard = heardList.remove(index);
                updateNode(oldHeard, heard);
                heardList.add(0, oldHeard);
            } else {
                // Add new node to list
                heardList.add(0, heard);
            }
        }

        // Keep the list at a max of 200 entries.
        if (heardList.size() > 200) {
            heardList.remove(heardList.size() - 1);
        }
    }

    /**
     * Update the existing node with the information from the updated one.
     *
     * @param oldNode
     * @param newNode
     */
    private void updateNode(Node oldNode, Node newNode) {
        oldNode.setLastHeard(newNode.getLastHeard());
        oldNode.setRssi(newNode.getRSSI());
        oldNode.setAnInterface(newNode.getInterface());
        for (Capability c : newNode.getCapabilities()) {
            oldNode.addCapabilityOrUpdate(c);
        }
    }

    @Subscribe
    public void heardNode(HeardNodeEvent heardNode) {
        // Quick validation of the callsign.
        if (!Tools.isValidITUCallsign(heardNode.getNode().getCallsign())) {
            return;
        }

        // Update the heard list with a copy.
        addToFront(new Node(heardNode.getNode()));
    }

}
