package org.prowl.kisset.statistics.types;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.protocols.core.Node;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.util.Tools;

import java.util.List;

/**
 * An unheard node represents a node that we have heard another node talking to
 * we will generally ignore SABM and DISC frames from them as that doesn't represent
 * an active connection, and we will be interested in RR, RNR, etc frames.
 */
public class UnHeard extends MHeard {

    private static final Log LOG = LogFactory.getLog("UnHeard");


    public UnHeard() {
        super();
    }

    @Subscribe
    public void heardNode(HeardNodeEvent heardNode) {

        String callsignToValidate = heardNode.getNode().getDestination();
        Node unheard = new Node(heardNode.getNode().getInterface(), callsignToValidate, heardNode.getNode().getLastHeard(), null, null);
        MHeard mHeard = KISSet.INSTANCE.getStatistics().getHeard();

        // If the callsign is in our heard list, then we don't add it here.
        List<Node> heard = mHeard.listHeard();
        if (heard.contains(unheard)) {
            return;
        }

        // Also if a callsign appears in our heard list then we remove it from the unheard list
        removeLocallyHeardNodes();

        // Now if the frame is present, then make sure we only bother with 'connected mode' frames which mean that both
        // sides have setup a connection and can see each other
        AX25Frame frame = heardNode.getNode().getFrame();
        if (frame != null) {
            if (frame.getFrameType() == AX25Frame.FRAMETYPE_S) {
                //   LOG.debug("Frame type:" + frame.getFrameType() + " SType:" + frame.getSType());
                int sType = frame.getSType();
                if (sType != AX25Frame.STYPE_RR && sType != AX25Frame.STYPE_RNR && sType != AX25Frame.STYPE_REJ) {
                    //     LOG.debug("Ignoring frame type:" + frame.getFrameType() + " SType:" + frame.getSType());
                    return;
                }
            } else {
                //   LOG.debug("Ignoring frame type:" + frame.getFrameType() + "   " + frame.toString());
                return;
            }
        }

        //We ignore our main callsigns.
        if (callsignToValidate.toLowerCase().startsWith(KISSet.INSTANCE.getMyCall().toLowerCase())) {
            return;
        }

        // Then just make sure the callsign is for a valid user per ITU callsign
        if (!Tools.isValidITUCallsign(callsignToValidate)) {
            return;
        }

        // For unheard nodes we create a node based on what we have heard and pass add it.
        unheard.addCanReachNodeOrReplace(new Node(heardNode.getNode()));
        addToFront(unheard);
    }

    public void removeLocallyHeardNodes() {
        MHeard mHeard = KISSet.INSTANCE.getStatistics().getHeard();
        List<Node> heard = mHeard.listHeard();
        for (Node node : heard) {
            heardList.remove(node);
        }
    }
}
