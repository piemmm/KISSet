package org.prowl.kisset.services.remote.netrom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.user.User;
import org.prowl.kisset.protocols.netrom.NetROMPacket;
import org.prowl.kisset.services.ClientHandler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;

public class NetROMClientHandler implements ClientHandler {

    private static final Log LOG = LogFactory.getLog("NetROMClientHandler");

    private static final String CR = "\r";

    private InputStream in;
    private OutputStream out;
    private final User user;
    private final Interface anInterface;
    private boolean colourEnabled = true;
    private BufferedReader bin;
    private NetROMService service;

    public NetROMClientHandler(NetROMService service, Interface anInterface, User user, InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.user = user;
        this.service = service;
        this.anInterface = anInterface;
    }

    @Override
    public void start() {

        try {
            byte[] buffer = new byte[512];
            int b = 0;
            while (b != -1) {

                // This will read an entire packet at a time.
                int lengthRead = in.read(buffer, 0, buffer.length);
                byte[] data = new byte[lengthRead];
                System.arraycopy(buffer, 0, data, 0, lengthRead);

                if (data.length > 0) {
                    // We have a packet, let's process it.
                    NetROMPacket packet = new NetROMPacket(data);
                    processPacket(packet);
                }
            }
        } catch (Throwable e) {
            LOG.error("Error in client handler", e);

        } finally {
            service.clientDisconnected(anInterface, user);
        }
    }


    /**
     * Process a netrom packet from a connected node according to the netrom spec.
     *
     * A connected node could be conversing with us about several different circuits/things
     * so we will farm each of these off to their own individual handlers where needed.
     *
     * @param packet
     */
    public void processPacket(NetROMPacket packet) {

        // We have a packet, let's process it.
        switch (packet.getOpCode()) {
            case NetROMPacket.OPCODE_CONNECT_REQUEST:
                // We have a connection request.
                LOG.debug("Got a connection request from " + packet.getOriginCallsign()+" to "+packet.getDestinationCallsign());
                break;
            case NetROMPacket.OPCODE_CONNECT_ACK:
                // We have a connection ack.
                LOG.debug("Got a connection ack from " + packet.getOriginCallsign()+" to "+packet.getDestinationCallsign());
                break;
            case NetROMPacket.OPCODE_DISCONNECT_REQUEST:
                // We have a disconnect request.
                LOG.debug("Got a disconnect request from " + packet.getOriginCallsign()+" to "+packet.getDestinationCallsign());
                break;
            case NetROMPacket.OPCODE_DISCONNECT_ACK:
                // We have a disconnect ack.
                LOG.debug("Got a disconnect ack from " + packet.getOriginCallsign()+" to "+packet.getDestinationCallsign());
                break;
            case NetROMPacket.OPCODE_INFORMATION_TRANSFER:
                // We have a data packet.
                LOG.debug("Got a data packet from " + packet.getOriginCallsign()+" to "+packet.getDestinationCallsign());
                break;
            case NetROMPacket.OPCODE_INFORMATION_ACK:
                // We have a data ack.
                LOG.debug("Got a data ack from " + packet.getOriginCallsign()+" to "+packet.getDestinationCallsign());
                break;
            case NetROMPacket.OPCODE_RESET:
                // We have a reset.
                LOG.debug("Got a reset from " + packet.getOriginCallsign()+" to "+packet.getDestinationCallsign());
                break;

        }


    }

    /**
     * This is the remote node that we are connected to
     *
     * @return
     */
    public User getUser() {
        return user;
    }
}