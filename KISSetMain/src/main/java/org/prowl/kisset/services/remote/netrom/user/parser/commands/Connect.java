package org.prowl.kisset.services.remote.netrom.user.parser.commands;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.AX25Callsign;
import org.prowl.ax25.ConnState;
import org.prowl.ax25.ConnectionEstablishmentListener;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.NodeCommand;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.routing.NetROMRoute;
import org.prowl.kisset.protocols.netrom.NetROMRoutingTable;
import org.prowl.kisset.services.remote.netrom.circuit.Circuit;
import org.prowl.kisset.services.remote.netrom.circuit.CircuitManager;
import org.prowl.kisset.services.remote.netrom.circuit.CircuitState;
import org.prowl.kisset.services.remote.netrom.opcodebeans.ConnectRequest;
import org.prowl.kisset.services.remote.netrom.server.NetROMClientHandler;
import org.prowl.kisset.services.remote.netrom.server.NetROMServerService;
import org.prowl.kisset.services.remote.netrom.user.parser.Mode;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Connect to a remote station.
 */
@NodeCommand
public class Connect extends Command {

    private static final Log LOG = LogFactory.getLog("Connect");


    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }


        if (data.length < 2 || data[1].equals("?")) {
            write("*** Usage: connect <station>" + CR);
            return true;
        }


        // Attempt a connection via the node network
        if (data.length == 2) {
            String station = data[1].toUpperCase();
            write("*** Connecting to " + station + CR);

            // See if it's a node.
            NetROMRoute route = NetROMRoutingTable.INSTANCE.getRoutingToCallsign(data[1].toUpperCase());
            if (route != null) {
                // Connect via the next hop node
                nodeConnect(route, station);
            } else {
                // No route to a node
                write("*** Unknown node: " + station + CR);
            }
        } else if (data.length == 3) {

            // Get the interface if it exists
            Interface anInterface = KISSet.INSTANCE.getInterfaceHandler().getInterface(Tools.getInteger(data[1], -1));
            if (anInterface == null) {
                write("*** Interface " + data[1] + " does not exist" + CR);
                return true;
            }
            // If an interface was specified then we are being asked to do a direct connection
            directConnect(anInterface, data[2].toUpperCase());

        } else {
            write("*** Usage: connect <station>" + CR);
        }

        return true;
    }


    /**
     * Connect to the specified net/rom node.
     */
    public void nodeConnect(NetROMRoute route, String toCallsign) throws IOException {

        // Get the interface we are going to use
        Interface routeInterface = route.getAnInterface();

        // Choose a suitable SSID for our outgoing callsign.
        //String callsign = chooseCallsign(client.getUser().getBaseCallsign());
        NetROMServerService netROMService = ((NetROMServerService) KISSet.INSTANCE.getService(NetROMServerService.class));


        // Create a circuit to handle the connection
        Circuit circuit = new Circuit();
        circuit.setSourceCallsign(new AX25Callsign(netROMService.getCallsign()));
        circuit.setDestinationCallsign(new AX25Callsign(toCallsign));
        circuit.setOriginatingUser(new AX25Callsign(client.getUser().getSourceCallsign()));
        circuit.setOriginatingNode(new AX25Callsign(client.getUser().getDestinationCallsign()));
        circuit.setAcceptedFrames(4); // Reasonable default.
        circuit.setState(CircuitState.CONNECTING);

        // Now initiate a NetROM<->NetROM connection
        NetROMClientHandler nextStation = netROMService.getClientHandlerForCallsign(route.getAnInterface(), new AX25Callsign(route.getSourceCallsign()), true);
        if (nextStation == null) {
            write("*** Node not in routing table: " + route.getSourceCallsign() + CR);
            return;
        }

        CircuitManager.registerCircuit(circuit, nextStation); // Applies the circuit IDs and indexes.

        ConnectRequest connectRequest = new ConnectRequest();
        connectRequest.setDestinationCallsign(circuit.getDestinationCallsign());
        connectRequest.setSourceCallsign(circuit.getSourceCallsign());
        connectRequest.setMyCircuitID(circuit.getMyCircuitId());
        connectRequest.setMyCircuitIndex(circuit.getMyCircuitIndex());
        connectRequest.setProposeWindowSize(circuit.getAcceptedFrames());
        connectRequest.setCallsignOfOriginatingUser(new AX25Callsign(client.getUser().getSourceCallsign()));
        connectRequest.setCallsignOfOriginatingNode(new AX25Callsign(netROMService.getCallsign()));

        nextStation.sendPacket(connectRequest.getNetROMPacket());

        // Wait for the connection to be established, or timeout
        long timesOutAt = System.currentTimeMillis() + NetROMClientHandler.TIMEOUT;
        while (circuit.getState() == CircuitState.CONNECTING) {
            if (System.currentTimeMillis() > timesOutAt) {
                write("*** Connection timed out" + CR);
                return;
            }
            Tools.delay(1000);
        }

        // We can now assume connected or disconnected state
        if (circuit.getState() == CircuitState.DISCONNECTED) {
            write("*** Connection failed" + CR);
            return;
        }


        try {
            // We are now connected.
            write("*** Connected to " + toCallsign + CR);
            commandParser.setMode(Mode.CONNECTED_TO_STATION);

            // Now setup the streams so we talk to the station instead of the command parser
            //stream.setIOStreams(conn.getInputStream(), conn.getOutputStream());

            OutputStream out = circuit.getCircuitOutputStream();

            // Create a thread to read from the station and send to the client
            commandParser.setDivertStream(out);
            Tools.runOnThread(() -> {
                try {
                    StringBuffer responseString = new StringBuffer();
                    while (true) {
                        InputStream in = circuit.getCircuitInputStream();
                        if (in.available() > 0) {
                            int b = in.read();
                            if (b == -1) {
                                break;
                            }
                            commandParser.writeRaw(b);
                        } else {
                            // Crude.
                            Tools.delay(500);
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Error reading from client:" + e.getMessage(), e);
                }

                try {
                    commandParser.setDivertStream(null);
                    commandParser.setMode(Mode.CMD);
                    commandParser.sendPrompt();
                } catch (IOException e) {
                    LOG.error("Error writing to client:" + e.getMessage(), e);
                }

            });

        } catch (IOException e) {
            commandParser.setDivertStream(null);
            LOG.error("Error writing to client:" + e.getMessage(), e);
        }


    }

    /**
     * Choose a callsign that has not been seen locally to use for the user
     *
     * @return
     */
    public String chooseCallsign(String baseCallsign) {
        // Choose a suitable SSID for our outgoing callsign.

        int i = 15;
        String chosenCall = null;
        do {
            chosenCall = baseCallsign + "-" + i;

        } while (KISSet.INSTANCE.getStatistics().getHeard().seen(chosenCall));

        return chosenCall;

    }

    /**
     * Connect via the specified interface to a remote station that is not a net/rom node.
     *
     * @param anInterface
     * @param callsign
     */
    public void directConnect(Interface anInterface, String callsign) throws IOException {
        String chosenCallsign = chooseCallsign(client.getUser().getBaseCallsign());

        write("*** Connecting to " + callsign + " as " + chosenCallsign);
        anInterface.connect(callsign.toUpperCase(), chosenCallsign, new ConnectionEstablishmentListener() {
            @Override
            public void connectionEstablished(Object sessionIdentifier, ConnState conn) {
                try {
                    commandParser.write(CR + "*** Connected to " + conn.getDst().toString().toUpperCase() + CR);
                    commandParser.setMode(Mode.CONNECTED_TO_STATION);


                    OutputStream out = conn.getOutputStream();

                    // Create a thread to read from the station and send to the client
                    commandParser.setDivertStream(out);
                    Tools.runOnThread(() -> {
                        try {
                            StringBuffer responseString = new StringBuffer();
                            while (true) {
                                InputStream in = conn.getInputStream();
                                if (in.available() > 0) {
                                    int b = in.read();
                                    if (b == -1) {
                                        break;
                                    }
                                    commandParser.writeRaw(b);
                                } else {
                                    // Crude.
                                    Tools.delay(100);
                                }
                            }
                        } catch (IOException e) {
                            LOG.error("Error reading from client:" + e.getMessage(), e);
                        }

                        try {
                            commandParser.setDivertStream(null);
                            commandParser.setMode(Mode.CMD);
                            commandParser.sendPrompt();
                        } catch (IOException e) {
                            LOG.error("Error writing to client:" + e.getMessage(), e);
                        }

                    });

                } catch (IOException e) {
                    commandParser.setDivertStream(null);
                    LOG.error("Error writing to client:" + e.getMessage(), e);
                }
            }

            @Override
            public void connectionNotEstablished(Object sessionIdentifier, Object reason) {
                try {
                    commandParser.write("*** Unable to connect: " + reason + CR);
                    commandParser.setMode(Mode.CMD);
                    commandParser.sendPrompt();
                } catch (IOException e) {
                    LOG.error("Error writing to client:" + e.getMessage(), e);
                }
            }

            @Override
            public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {
                try {
                    commandParser.write("*** Connection closed. " + CR);
                    commandParser.setMode(Mode.CMD);
                    commandParser.sendPrompt();
                } catch (IOException e) {
                    LOG.error("Error writing to client:" + e.getMessage(), e);
                }
            }

            @Override
            public void connectionLost(Object sessionIdentifier, Object reason) {
                try {
                    commandParser.write("*** Connection lost. " + CR);
                    commandParser.setMode(Mode.CMD);
                    commandParser.sendPrompt();
                } catch (IOException e) {
                    LOG.error("Error writing to client:" + e.getMessage(), e);
                }
            }
        });
    }


    @Override
    public String[] getCommandNames() {
        return new String[]{"c", "connect"};
    }


}
