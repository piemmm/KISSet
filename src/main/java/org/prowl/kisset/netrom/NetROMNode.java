package org.prowl.kisset.netrom;

import org.prowl.kisset.io.Interface;

import java.text.SimpleDateFormat;

public class NetROMNode {

    public String sourceCallsign; // Source node this route came from
    public Interface anInterface;
    public String destinationNodeCallsign;
    public String destinationNodeMnemonic;
    public String neighbourNodeCallsign;
    public int bestQualityValue;
    public long lastHeard;

    public NetROMNode(Interface anInterface, String sourceCallsign, String destinationNodeCallsign, String destinationNodeMnemonic, String neighbourNodeCallsign, int bestQualityValue) {
        this.anInterface = anInterface;
        this.destinationNodeCallsign = destinationNodeCallsign;
        this.destinationNodeMnemonic = destinationNodeMnemonic;
        this.neighbourNodeCallsign = neighbourNodeCallsign;
        this.bestQualityValue = bestQualityValue;
        this.sourceCallsign = sourceCallsign;
        this.lastHeard = System.currentTimeMillis();
    }

    public Interface getAnInterface() {
        return anInterface;
    }

    public String getSourceCallsign() {
        return sourceCallsign;
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

    public long getLastHeard() {
        return lastHeard;
    }

    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        StringBuilder builder = new StringBuilder();
        builder.append(sourceCallsign);
        builder.append(" advertises");
        builder.append(" "+destinationNodeCallsign+"/"+destinationNodeMnemonic);
        builder.append(" via ");
        builder.append(neighbourNodeCallsign);
        builder.append(" quality ");
        builder.append(bestQualityValue);
        builder.append(" heard at "+sdf.format(lastHeard));
        return builder.toString();
    }
}
