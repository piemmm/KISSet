package org.prowl.kisset.comms.host.parser.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.ax25.ConnState;
import org.prowl.kisset.ax25.ConnectionEstablishmentListener;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Connect to a remote station and then the TNC will be in a connect mode.
 */
@TNCCommand
public class Connect extends Command implements ConnectionEstablishmentListener {

    private static final Log LOG = LogFactory.getLog("Connect");


    @Override
    public boolean doCommand(String[] data) throws IOException {
        if (!getMode().equals(Mode.CMD)) {
            return false;
        }

        if (data.length < 2) {
            write("*** Usage: connect [<port number>] <station>" + CR);
            return true;
        }

        if (data.length == 2) {
            String station = data[1];
            Interface anInterface = KISSet.INSTANCE.getInterfaceHandler().getInterface(0);
            if (anInterface == null) {
                write("*** No interfaces configured" + CR);
            } else {
                write("*** Connecting to " +data[1].toUpperCase()+ CR);

                anInterface.connect(data[1].toUpperCase(), KISSet.INSTANCE.getMyCall(), this);
            }

        } else if (data.length == 3) {
//            int port = Integer.parseInt(data[1]);
//            String station = data[2];
//            KISSet.INSTANCE.getInterfaceHandler().connect(port, station);
        }

        write(CR);

        return true;
    }

    @Override
    public void connectionEstablished(Object sessionIdentifier, ConnState conn) {
        try {
            write("*** Connected to " + conn.getDst().toString().toUpperCase() + CR);
            setMode(Mode.CONNECTED_TO_STATION);

            // Now setup the streams so we talk to the station instead of the command parser
            InputStream in = conn.getInputStream();
            OutputStream out = conn.getOutputStream();

            commandParser.setDivertStream(out);
            Tools.runOnThread(() -> {
                try {
                    while(true) {
                        int b = in.read();
                        if(b == -1) {
                            break;
                        }
                        if (b == 13) {
                            tncHost.send("\n");
                        }
                        tncHost.send(String.valueOf((char)b));
                    }
                } catch(IOException e) {
                    LOG.error("Error reading from client:"+e.getMessage(), e);
                }
                commandParser.setDivertStream(null);
                setMode(Mode.CMD);

            });
        } catch(IOException e) {
            commandParser.setDivertStream(null);
            LOG.error("Error writing to client:"+e.getMessage(), e);
        }
    }

    @Override
    public void connectionNotEstablished(Object sessionIdentifier, Object reason) {
        try {
            write("*** Unable to connect: " +reason + CR);
            setMode(Mode.CONNECTED_TO_STATION);
        } catch(IOException e) {
            LOG.error("Error writing to client:"+e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    @Override
    public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {
        try {
            write("*** Connection closed"+ CR);
            setMode(Mode.CMD);
        } catch(IOException e) {
            LOG.error("Error writing to client:"+e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    @Override
    public void connectionLost(Object sessionIdentifier, Object reason) {
        try {
            write("*** Lost connection: " + reason + CR);
            setMode(Mode.CMD);
        } catch(IOException e) {
            LOG.error("Error writing to client:"+e.getMessage(), e);
        }
        commandParser.setDivertStream(null);
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"c", "connect"};
    }

}
