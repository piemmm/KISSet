package org.prowl.kisset.objects.routing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.InvalidMessageException;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.util.Tools;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.text.SimpleDateFormat;

public class NetROMRoute {
    private static final Log LOG = LogFactory.getLog("NetROMRoute");

    private static final long MAX_AGE = 1000 * 60 * 60 * 2; // 2 hours

    public String sourceCallsign; // Source node this route came from
    public Interface anInterface;
    public String destinationNodeCallsign;
    public String destinationNodeMnemonic;
    public String neighbourNodeCallsign;
    public int bestQualityValue;
    public long lastHeard;


    public NetROMRoute() {

    }

    public NetROMRoute(Interface anInterface, String sourceCallsign, String destinationNodeCallsign, String destinationNodeMnemonic, String neighbourNodeCallsign, int bestQualityValue) {
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

    public void setSourceCallsign(String sourceCallsign) {
        this.sourceCallsign = sourceCallsign;
    }

    public void setAnInterface(Interface anInterface) {
        this.anInterface = anInterface;
    }

    public void setDestinationNodeCallsign(String destinationNodeCallsign) {
        this.destinationNodeCallsign = destinationNodeCallsign;
    }

    public void setDestinationNodeMnemonic(String destinationNodeMnemonic) {
        this.destinationNodeMnemonic = destinationNodeMnemonic;
    }

    public void setNeighbourNodeCallsign(String neighbourNodeCallsign) {
        this.neighbourNodeCallsign = neighbourNodeCallsign;
    }

    public void setBestQualityValue(int bestQualityValue) {
        this.bestQualityValue = bestQualityValue;
    }

    public void setLastHeard(long lastHeard) {
        this.lastHeard = lastHeard;
    }

    /**
     * Show a human readable version of the routing packet
     *
     * @return
     */
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        StringBuilder builder = new StringBuilder();
        if (anInterface == null) {
            builder.append("Unknown interface");
        } else {
            int interfaceNumber = KISSet.INSTANCE.getInterfaceHandler().getInterfaceNumber(anInterface);
            builder.append(interfaceNumber + ": ");
        }
        builder.append(sourceCallsign);
        builder.append(" advertises");
        builder.append(" " + destinationNodeCallsign + "/" + destinationNodeMnemonic);
        builder.append(" neighbour ");
        builder.append(neighbourNodeCallsign);
        builder.append(" quality ");
        builder.append(bestQualityValue);
        builder.append(" heard at " + sdf.format(lastHeard));
        return builder.toString();
    }


    /**
     * Serialise into a byte array. Keeping the size to a minimum is important.
     * Length, data, length, data format for all the fields.
     *
     * @return A byte array representing the serialised message
     */
    public byte[] toSerialize() {

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(30);
             DataOutputStream dout = new DataOutputStream(bos)) {

            Storage.write(dout, anInterface.getUUID());
            Storage.write(dout, sourceCallsign);
            Storage.write(dout, destinationNodeCallsign);
            Storage.write(dout, destinationNodeMnemonic);
            Storage.write(dout, neighbourNodeCallsign);
            Storage.write(dout, bestQualityValue);
            Storage.write(dout, lastHeard);

            dout.flush();
            dout.close();
            return bos.toByteArray();

        } catch (Throwable e) {
            LOG.error("Unable to serialise message", e);
        }
        return null;
    }


    /**
     * Deserialise from a byte array
     **/
    public NetROMRoute fromSerialize(DataInputStream din) throws InvalidMessageException {

        try {
            anInterface = KISSet.INSTANCE.getInterfaceHandler().getInterfaceByUUID(Tools.readString(din, din.readInt()));
            sourceCallsign = Tools.readString(din, din.readInt());
            destinationNodeCallsign = Tools.readString(din, din.readInt());
            destinationNodeMnemonic = Tools.readString(din, din.readInt());
            neighbourNodeCallsign = Tools.readString(din, din.readInt());
            bestQualityValue = din.readInt();
            lastHeard = din.readLong();

        } catch (Throwable e) {
            LOG.error("Unable to build message from packet", e);
            throw new InvalidMessageException(e.getMessage(), e);
        }
        return this;
    }

    public boolean isExipred() {
        return anInterface == null || (System.currentTimeMillis() - lastHeard) > MAX_AGE;
    }
}
