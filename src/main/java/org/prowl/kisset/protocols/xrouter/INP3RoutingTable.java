package org.prowl.kisset.protocols.xrouter;

import org.prowl.kisset.objects.routing.INP3Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum INP3RoutingTable {

    INSTANCE;

    private final List<INP3Route> nodes = new ArrayList<>();

    /**
     * Add a node to the routing table, replacing any existing node with the same callsign
     * @param node
     */
    public void addRoute(INP3Route node) {

        // Remove any existing node with the same callsign
        for (INP3Route existingNode : nodes) {
            if (existingNode.getDestinationNodeCallsign().equals(node.getDestinationNodeCallsign())
             && existingNode.getSourceCallsign().equals(node.getSourceCallsign())) {
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
    public void addRoutes(List<INP3Route> nodes) {
        for (INP3Route n: nodes) {
            addRoute(n);
        }
    }

    public void removeNode(INP3Route node) {
        nodes.remove(node);
    }

    public List<INP3Route> getNodes() {
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
    public INP3Route getRoutingToCallsign(String callsignToConnectTo) {
        callsignToConnectTo = callsignToConnectTo.toUpperCase(Locale.ENGLISH);
        INP3Route bestNode = null;
        for (INP3Route node : nodes) {
            if (node.getDestinationNodeCallsign().equals(callsignToConnectTo) ||
                    (node.hasAlias() && node.getAlias().equals(callsignToConnectTo))) {
                if (bestNode == null) {
                    bestNode = node;
                } else {
                    if (node.getTripTime() < bestNode.getTripTime()) {
                        bestNode = node;
                    }
                }
            }
        }
        return bestNode;
    }

}
