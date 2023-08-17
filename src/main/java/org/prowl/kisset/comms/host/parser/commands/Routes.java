package org.prowl.kisset.comms.host.parser.commands;

import org.apache.commons.lang.StringUtils;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.core.Capability;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.netrom.NetROMNode;
import org.prowl.kisset.netrom.RoutingTable;
import org.prowl.kisset.statistics.types.MHeard;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * List the known routes heard from various other nodes.
 */
@TNCCommand
public class Routes extends Command {

    private static final long HOUR = 1000 * 60 * 60;
    private static final long TWO_HOUR = 1000 * 60 * 60 * 2;


    @Override
    public boolean doCommand(String[] data) throws IOException {

        if (!getMode().equals(Mode.CMD)) {
            return false;
        }
        writeToTerminal(CR);

        // Get the routes and interate
        List<NetROMNode> nodes = RoutingTable.INSTANCE.getNodes();
        if (nodes.size()> 0) {
            writeToTerminal(ANSI.BOLD+ANSI.UNDERLINE+"List of Net/ROM routes seen from local nodes:"+ANSI.NORMAL + CR);

            long now = System.currentTimeMillis();
            for (NetROMNode node : nodes) {

                String color;
                if (now - node.getLastHeard() < HOUR) {
                    color = ANSI.GREEN;
                } else if (now - node.getLastHeard() < TWO_HOUR) {
                    color = ANSI.YELLOW;
                } else {
                    color = ANSI.RED;
                }
                writeToTerminal(color);
                writeToTerminal(node.toString() + ANSI.NORMAL+CR);
            }
        } else {
            writeToTerminal("***  No routes seen yet" + CR);
        }

        return true;
    }



    @Override
    public String[] getCommandNames() {
        return new String[]{"routes", "route", "ro", "rou"};
    }


}
