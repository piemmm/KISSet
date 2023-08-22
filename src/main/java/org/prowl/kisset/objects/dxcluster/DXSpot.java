package org.prowl.kisset.objects.dxcluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.DXSpotEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * A DX spot
 *
 * A DX spot looks like:
 * DX de G4ABC:     14074.0  EA1AHA        FT8   0 dB  1236 Hz
 */
public class DXSpot {
    private static final Log LOG = LogFactory.getLog("DXSpot");

    String dxLine;
    String sourceCallsign;

    String dxFrom; // The reporting callsign
    String dxSpotted; // The spooted callsign
    String comment; // Any comments;
    double frequency; // The frequency in MHz
    String mode; // The mode(if it can be parsed from the comemnt)
    long spotTime; // The time the spot was made

    public DXSpot(Node node) {

        SimpleDateFormat sdf = new SimpleDateFormat("HHmm");

        sourceCallsign = node.getCallsign();

        dxLine = new String(node.getFrame().getBody());

        dxFrom = dxLine.substring(dxLine.indexOf("DX de ") + 6, dxLine.indexOf(":"));
        frequency = Double.parseDouble(dxLine.substring(dxLine.indexOf(":") + 1, 24).trim());
        dxSpotted = dxLine.substring(25, dxLine.indexOf(" ", 26)).trim();
        comment = dxLine.substring(dxLine.indexOf(" ", 26)+1).trim();
        comment = comment.substring(0, comment.length()-5).trim();
        try {
            spotTime = sdf.parse(dxLine.substring(dxLine.length() - 5, dxLine.length()-1)).getTime();
        } catch (ParseException e) {
            LOG.error(e.getMessage(),e);
            spotTime = node.getLastHeard();
        }

        DXSpotEvent event = new DXSpotEvent(this);
        SingleThreadBus.INSTANCE.post(event);
    }

    public String getLine() {
        return dxLine;
    }

}
