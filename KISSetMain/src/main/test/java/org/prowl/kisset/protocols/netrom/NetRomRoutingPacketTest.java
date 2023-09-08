package org.prowl.kisset.protocols.netrom;


import org.junit.Assert;
import org.junit.Test;
import org.prowl.ax25.AX25Frame;
import org.prowl.kisset.objects.routing.NetROMRoute;
import org.prowl.kisset.protocols.core.Node;
import org.prowl.kisset.protocols.netrom.NetROMRoutingPacket;

import java.text.ParseException;

public class NetRomRoutingPacketTest {

    @Test
    public void testPacket() {

        NetROMRoute testRoute = new NetROMRoute(null, "G0ABC", "G1BCD", "BCDNOD", "G2NBR", 56);

        NetROMRoutingPacket packet = new NetROMRoutingPacket();
        packet.addNode(testRoute);

        System.out.println(testRoute.toString());

        byte[] data = packet.toPacketBody("G1SND");


        AX25Frame frame = new AX25Frame();
        frame.body = data;
        Node node = new Node(null, "G6DEF", System.currentTimeMillis(), "NODES", frame);
        try {
            NetROMRoutingPacket packet2 = new NetROMRoutingPacket(node);

            System.out.println(packet2.toString());

        } catch (ParseException e) {
            Assert.fail(e.getMessage());
        }


    }


}
