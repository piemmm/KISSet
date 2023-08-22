package org.prowl.kisset.protocols.netrom;

import org.prowl.kisset.objects.routing.NetROMRoute;

import java.util.ArrayList;
import java.util.List;

public enum NetROMRoutingTable {

    INSTANCE;

    private final List<NetROMRoute> nodes = new ArrayList<>();

    /**
     * Add a node to the routing table, replacing any existing node with the same callsign
     * @param node
     */
    public void addRoute(NetROMRoute node) {

        // Remove any existing node with the same callsign
        for (NetROMRoute existingNode : nodes) {
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
    public void addRoutes(List<NetROMRoute> nodes) {
        for (NetROMRoute n: nodes) {
            addRoute(n);
        }
    }

    public void removeNode(NetROMRoute node) {
        nodes.remove(node);
    }

    public List<NetROMRoute> getNodes() {
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
    public NetROMRoute getRoutingToCallsign(String callsignToConnectTo) {
        NetROMRoute bestNode = null;
        for (NetROMRoute node : nodes) {
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
