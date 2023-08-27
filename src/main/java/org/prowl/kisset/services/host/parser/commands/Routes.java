package org.prowl.kisset.services.host.parser.commands;

import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.objects.routing.NetROMRoute;
import org.prowl.kisset.protocols.netrom.NetROMRoutingTable;
import org.prowl.kisset.util.ANSI;

import java.io.IOException;
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
        List<NetROMRoute> nodes = NetROMRoutingTable.INSTANCE.getNodes();
        if (nodes.size()> 0) {
            writeToTerminal(ANSI.BOLD+ANSI.UNDERLINE+"List of Net/ROM routes seen from local nodes:"+ANSI.NORMAL + CR);

            long now = System.currentTimeMillis();
            for (NetROMRoute node : nodes) {

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
