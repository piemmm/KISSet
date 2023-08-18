package org.prowl.kisset.netrom;

import java.util.ArrayList;
import java.util.List;

public enum RoutingTable {

    INSTANCE;

    private final List<NetROMNode> nodes = new ArrayList<>();

    /**
     * Add a node to the routing table, replacing any existing node with the same callsign
     * @param node
     */
    public void addNode(NetROMNode node) {

        // Remove any existing node with the same callsign
        for (NetROMNode existingNode : nodes) {
            if (existingNode.getDestinationNodeCallsign().equals(node.getDestinationNodeCallsign())
             && existingNode.getNeighbourNodeCallsign().equals(node.getNeighbourNodeCallsign())
            && existingNode.getDestinationNodeMnemonic().equals(node.getDestinationNodeMnemonic())) {
                nodes.remove(existingNode);
                break;
            }
        }

        nodes.add(0,node);
    }

    /**
     * Add a list of nodes to the routing table, replacing any existing nodes with the same callsign
     * @param nodes
     */
    public void addNodes(List<NetROMNode> nodes) {
        for (NetROMNode n: nodes) {
            addNode(n);
        }
    }

    public void removeNode(NetROMNode node) {
        nodes.remove(node);
    }

    public List<NetROMNode> getNodes() {
        return nodes;
    }

    public void clear() {
        nodes.clear();
    }

}
