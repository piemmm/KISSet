package org.prowl.kisset.services.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.protocols.core.Node;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.statistics.types.MHeard;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@TNCCommand
public class UHeard extends Command {

    @Override
    public boolean doCommand(String[] data) throws IOException {

        // We're only interesteed in comamnd moed.
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        writeToTerminal(CR);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm:ss");

        MHeard heard = KISSet.INSTANCE.getStatistics().getUnHeard();
        List<Interface> connectors = KISSet.INSTANCE.getInterfaceHandler().getInterfaces();
        List<Node> nodes = heard.listHeard();
        if (nodes.size() == 0) {
            writeToTerminal("*** No nearby nodes unheard yet" + CR);
        } else {
            writeToTerminal(ANSI.UNDERLINE + ANSI.BOLD + "Int  Callsign  Last UnHeard       CanReach" + ANSI.NORMAL + CR);

            for (Node node : nodes) {
                String rssi = "-" + node.getRSSI() + " dBm";
                if (node.getRSSI() == Double.MAX_VALUE) {
                    rssi = "-  ";
                }

                writeToTerminal(StringUtils.rightPad(Integer.toString(connectors.indexOf(node.getInterface())), 5) + StringUtils.rightPad(node.getCallsign(), 10) + StringUtils.rightPad(sdf.format(node.getLastHeard()), 18) + " " + StringUtils.rightPad(canReach(node), 14) + CR);
            }
        }
        return true;
    }


    /**
     * Returns a nice string of callsigns that can reach this node.
     *
     * @param node
     * @return
     */
    public String canReach(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node n : node.getCanReachNodes()) {
            sb.append(n.getCallsign());
            sb.append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"unheard", "uh"};
    }


}
