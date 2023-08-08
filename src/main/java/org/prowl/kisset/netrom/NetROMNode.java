package org.prowl.kisset.netrom;

import org.prowl.kisset.io.Interface;

public class NetROMNode {

    public Interface anInterface;
    public String destinationNodeCallsign;
    public String destinationNodeMnemonic;
    public String neighbourNodeCallsign;
    public int bestQualityValue;

    public NetROMNode(Interface anInterface, String destinationNodeCallsign, String destinationNodeMnemonic, String neighbourNodeCallsign, int bestQualityValue) {
        this.anInterface = anInterface;
        this.destinationNodeCallsign = destinationNodeCallsign;
        this.destinationNodeMnemonic = destinationNodeMnemonic;
        this.neighbourNodeCallsign = neighbourNodeCallsign;
        this.bestQualityValue = bestQualityValue;
    }

    public Interface getAnInterface() {
        return anInterface;
    }

    public String getDestinationNodeCallsign() {
        return destinationNodeCallsign;
    }

    public String getDestinationNodeMnemonic() {
        return destinationNodeMnemonic;
    }

    public String getNeighbourNodeCallsign() {
        return neighbourNodeCallsign;
    }

    public int getBestQualityValue() {
        return bestQualityValue;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(" "+destinationNodeCallsign+"/"+destinationNodeMnemonic);
        builder.append(" via ");
        builder.append(neighbourNodeCallsign);
        builder.append(" quality ");
        builder.append(bestQualityValue);
        return builder.toString();
    }
}
