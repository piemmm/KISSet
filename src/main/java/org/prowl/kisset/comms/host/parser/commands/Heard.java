package org.prowl.kisset.comms.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.core.Capability;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.statistics.types.MHeard;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@TNCCommand
public class Heard extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        if (!getMode().equals(Mode.CMD)) {
            return false;
        }
        writeToTerminal(CR);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm:ss");
        MHeard heard = KISSet.INSTANCE.getStatistics().getHeard();
        List<Interface> connectors = KISSet.INSTANCE.getInterfaceHandler().getInterfaces();
        List<Node> nodes = heard.listHeard();
        if (nodes.size() == 0) {
            writeToTerminal("*** No nodes heard" + CR);
        } else {
            writeToTerminal(ANSI.UNDERLINE + ANSI.BOLD + "Int  Callsign  Last Heard         Capabilities" + ANSI.NORMAL + CR);
            for (Node node : nodes) {
                writeToTerminal(StringUtils.rightPad(Integer.toString(connectors.indexOf(node.getInterface())), 5) + StringUtils.rightPad(node.getCallsign(), 10) + StringUtils.rightPad(sdf.format(node.getLastHeard()), 18) + " " + StringUtils.rightPad(listCapabilities(node), 14) + CR);
            }
        }


        return true;
    }


    /**
     * Returns a string list of capability names this node has been seen to perform.
     *
     * @param node
     * @return
     */
    public String listCapabilities(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Capability c : node.getCapabilities()) {
            sb.append(c.getService().getName());
            sb.append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }



    @Override
    public String[] getCommandNames() {
        return new String[]{"mheard", "mh", "heard"};
    }


}
