package org.prowl.kisset.comms.host;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.Messages;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.comms.host.parser.commands.ChangeInterface;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.gui.terminals.ANSITerminal;
import org.prowl.kisset.gui.terminals.Terminal;
import org.prowl.kisset.gui.terminals.TerminalHost;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.io.StreamState;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.io.*;
import java.util.List;

/**
 * Each TNC host window is represented by an instance of this class.
 */
public class TNCHost {
    // The end character for the TNC prompt
    public static final String CR = CommandParser.CR;
    // Logging
    private static final Log LOG = LogFactory.getLog("TNCHost");
    // The command parser
    private final CommandParser parser;

    // The controller for this terminal (the host class)
    private TerminalHost host;

    // Terminal window input stream
    private final InputStream in;
    // Terminal window output stream
    private final OutputStream out;

    // True if the TNC monitor mode is enabled
    private boolean monitorEnabled;

    /**
     * Create a new TNC host.
     *
     * @param in  The input stream for the terminal window.
     * @param out The output stream for the terminal window.
     */
    public TNCHost(TerminalHost host, InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.host = host;
        this.parser = new CommandParser(this);

        // Get monitor state
        monitorEnabled = KISSet.INSTANCE.getConfig().getConfig(Conf.monitor, Conf.monitor.boolDefault());

        SingleThreadBus.INSTANCE.register(this);
        start();
    }

    public Terminal getTerminalType() {
        return host.getTerminal();
    }

    // Convenience method for setStatus on the terminalHost
    public void updateStatus() {
        String status = Messages.get("idle");

        if (parser.getCurrentInterface() != null && parser.getCurrentInterface().getCurrentStream() != null) {
            StreamState streamState = parser.getCurrentInterface().getCurrentStream().getStreamState();
            if (streamState != null && streamState == StreamState.CONNECTED) {
                status = streamState.toString() + " " + parser.getCurrentInterface().getCurrentStream().getRemoteCall();
            } else {
                status = streamState.toString();
            }
        }

        host.setStatus(status);
    }

    public void setTerminalType(Terminal terminal) {
        host.setTerminal(terminal);
    }

    public void setMode(Mode mode, boolean sendPrompt) {
        parser.setMode(mode, sendPrompt);
    }

    // Start the TNC host
    public void start() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                try {
                    // Write welcome/init message to the client.

                    send(CR + CR + CR);
                    send(Messages.get("tncInit") + CR);

                    updateStatus();

                    Platform.runLater(() -> {
                        Tools.delay(1000);
                        try {
                            checkConfiguration();
                            send("");
                            send(Messages.get("tncHelp") + CR);
                            send(parser.getPrompt());
                        } catch (IOException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    });

                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });

        // Start the reader thread for the client.
        Tools.runOnThread(() -> {
            try {
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader bin = new BufferedReader(reader);
                String inLine;
                while ((inLine = bin.readLine()) != null) {
                    parser.parse(inLine);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });


    }

    /**
     * Send ASCII text data to the client - will strip colour codes if user has requested it.
     *
     * @param data
     * @throws IOException
     */
    public void send(String data) throws IOException {
        data = data.replaceAll("[^\\x04-\\xFF]", "?");


        // Colourise
        data = ANSI.convertTokensToANSIColours(data);

        // If we're not using an ANSI terminal, then strip our hard colour codes.
        if (!(getTerminalType() instanceof ANSITerminal)) {
            data = ANSI.stripAnsiCodes(data);
        }

        out.write(data.getBytes());
    }

    /**
     * Flush the output stream.
     *
     * @throws IOException
     */
    public void flush() throws IOException {
        out.flush();
    }


    public void checkConfiguration() {
        try {
            // Go through each interface and check for a fail reason
            // If there is a fail reason, then we need to display a warning to the user.
            List<Interface> interfaces = KISSet.INSTANCE.getInterfaceHandler().getInterfaces();
            if (interfaces.size() > 0) {

                send(ANSI.BOLD + "Configured Interfaces:" + ANSI.NORMAL);
                ChangeInterface changeInterface = new ChangeInterface();
                changeInterface.setClient(this, null);
                changeInterface.showInterfaces();

            } else {
                send(ANSI.YELLOW + "*** No interfaces configured - please set one up in the preferences" + ANSI.NORMAL + CR);
            }


            if (KISSet.INSTANCE.getMyCallNoSSID() == null || KISSet.INSTANCE.getMyCallNoSSID().length() < 2) {
                send(ANSI.YELLOW + "*** No callsign configured - please set one in the preferences" + ANSI.NORMAL + CR);
            } else {
                String callsignNoSSID = KISSet.INSTANCE.getMyCallNoSSID();
                send(ANSI.BOLD + "Configured Callsign: " + ANSI.NORMAL);
                send(KISSet.INSTANCE.getMyCall());
                // Also show PMS if enabled
                if (KISSet.INSTANCE.getConfig().getConfig(Conf.pmsEnabled, Conf.pmsEnabled.boolDefault())) {
                    send(ANSI.BOLD + ", Mailbox/PMS: " + ANSI.NORMAL);
                    send(callsignNoSSID);
                    send(KISSet.INSTANCE.getConfig().getConfig(Conf.pmsSSID, Conf.pmsSSID.stringDefault()));
                }
                send(CR);
            }

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void setMonitor(boolean enabled) {

        this.monitorEnabled = enabled;

        // Update config
        KISSet.INSTANCE.getConfig().setProperty(Conf.monitor, enabled);
        KISSet.INSTANCE.getConfig().saveConfig();
    }

    public boolean isMonitorEnabled() {
        return monitorEnabled;
    }


    /**
     * Show packets when monitor mode is enabled
     *
     * @param event
     */
    @Subscribe
    public void packetReceived(HeardNodeEvent event) {
        // Monitor must be enabled
        if (!monitorEnabled) {
            return;
        }
        // And we should be in command mode
        if (parser.getMode() != Mode.CMD) {
            return;
        }
        // As well as being disconnected in the currently selected stream
        if (parser.getCurrentInterface() != null && !parser.getCurrentInterface().getCurrentStream().getStreamState().equals(StreamState.DISCONNECTED)) {
            return;
        }

        try {
            send(PacketTools.monitorPacketToString(event));
            flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
