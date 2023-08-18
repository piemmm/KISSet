package org.prowl.kisset.netrom;

import org.prowl.kisset.objects.netrom.NetROMNode;

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


    /**
     * Get the best quality route to a callsign. If there are multiple routes with the same quality, the highest quality route is returned.
     *
     * @param callsignToConnectTo
     * @return The first hop to the callsign, or null if no route is found
     */
    public NetROMNode getRoutingToCallsign(String callsignToConnectTo) {
        NetROMNode bestNode = null;
        for (NetROMNode node : nodes) {
            if (node.getDestinationNodeCallsign().equals(callsignToConnectTo) || node.getDestinationNodeMnemonic().equals(callsignToConnectTo)) {
                if (bestNode == null) {
                    bestNode = node;
                } else {
                    if (node.getBestQualityValue() > bestNode.getBestQualityValue()) {
                        bestNode = node;
                    }
                }
            }
        }
        return bestNode;
    }

}
