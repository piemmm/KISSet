package org.prowl.kisset;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.io.InterfaceHandler;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.protocols.RoutingListener;
import org.prowl.kisset.protocols.aprs.APRSISClient;
import org.prowl.kisset.protocols.aprs.APRSListener;
import org.prowl.kisset.protocols.dxcluster.DXListener;
import org.prowl.kisset.protocols.mqtt.MQTTClient;
import org.prowl.kisset.services.Service;
import org.prowl.kisset.services.host.TNCHost;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.services.remote.netrom.NetROMService;
import org.prowl.kisset.services.remote.pms.PMSService;
import org.prowl.kisset.statistics.Statistics;
import org.prowl.kisset.userinterface.TerminalHost;
import org.prowl.kisset.userinterface.stdinout.StdANSI;
import org.prowl.kisset.userinterface.stdinout.StdANSIWindowed;
import org.prowl.kisset.userinterface.stdinout.StdTeletext;
import org.prowl.kisset.userinterface.stdinout.StdTerminal;
import org.prowl.kisset.util.LoopingCircularBuffer;
import org.prowl.kisset.util.PipedIOStream;
import org.prowl.kisset.util.Tools;
import sun.misc.Signal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class KISSet {


    private static Log LOG;


    public static KISSet INSTANCE;
    public String myCall = "";
    protected List<Service> serviceList = Collections.synchronizedList(new ArrayList<>());
    private Config configuration;
    private InterfaceHandler interfaceHandler;
    private Statistics statistics;
    private Storage storage;
    private OutputStream stdOut;
    private InputStream stdIn;
    private StdTerminal terminal = null;

    // Breakout for our connection incoming data so we can intercept it for our buffer.
    private PipedIOStream inpis;

    private TNCHost tncHost;

    // Buffer data incoming so we can replay it on a different terminal when there is a change.
    private final LoopingCircularBuffer dataBuffer = new LoopingCircularBuffer(10240);

    /**
     * If this is true, then we are running in a terminal
     */
    public static boolean terminalMode = false;

    public KISSet() {
        super();
        INSTANCE = this;
        SingleThreadBus.INSTANCE.register(this);
    }

    public static void main(String[] args) {
        terminalMode = true;
        // Default terminal type for terminal selections.
        Class terminalType = StdANSI.class;

        // Parse command line arguments
        for (String s : args) {
            String setting = "";
            if (s.startsWith("--")) {
                setting = s.substring(2);
            } else if (s.startsWith("-")) {
                setting = s.substring(1);
            }

            if (s.contains("=")) {
                // property=value setting
                String[] parts = s.split("=");
                if (parts.length == 2) {
                    // System.setProperty(parts[0], parts[1]);
                }
            } else {
                // just a seting like --terminal or something like that
                if (setting.equalsIgnoreCase("ansi")) {
                    terminalMode = true;
                } else if (setting.equalsIgnoreCase("curses")) {
                    terminalType = StdANSIWindowed.class;
                    terminalMode = true;
                } else if (setting.equalsIgnoreCase("teletext")) {
                    terminalType = StdTeletext.class;
                    terminalMode = true;
                } else if (setting.equalsIgnoreCase("help") || setting.equalsIgnoreCase("h") || setting.equalsIgnoreCase("?")) {
                    System.out.println("KISSet command line options:");
                    System.out.println("--ansi - Runs in ANSI terminal mode (default)");
                    System.out.println("--curses - Runs in curses terminal mode");
                    System.out.println("--teletext - Runs in teletext/prestel terminal mode");
                    System.out.println("--help - Show this help");
                    System.exit(0);
                }
            }
        }

        // Main class holding non-gui stuff.
        KISSet kisset = new KISSet();
        kisset.initTerminalMode(terminalType);
    }


    public void initAll() {
        try {
            LOG = LogFactory.getLog("KISSet");

            // Init resource bundles.
            Messages.init();

            // This will always be desktop=
            System.setProperty("javafx.platform", "Desktop");

            // Statistics (heard, etc)
            statistics = new Statistics();

            // Stop any currenty running interfaces if this is a reload of the config
            if (interfaceHandler != null) {
                interfaceHandler.stop();
            }

            // Load configuration and initialise everything needed.
            if (configuration == null) {
                configuration = new Config();
            } else {
                configuration.loadConfig();
            }

            // Create our storage handler
            storage = new Storage();

            // Set our callsign
            myCall = configuration.getConfig(Conf.callsign, Conf.callsign.stringDefault()).toUpperCase(Locale.ENGLISH);

            // Create services - these listen for incoming connections and handle them.
            createServices();

            // Init interfaces
            interfaceHandler = new InterfaceHandler(configuration.getConfig("interfaces"));

            interfaceHandler.setServices(serviceList);

            // Start interfaces
            interfaceHandler.start();

            // Start listening for route broadcasts
            RoutingListener routingListener = RoutingListener.INSTANCE;

            // DX listener
            DXListener dxListener = DXListener.INSTANCE;

            // APRS listener
            APRSListener aprsListener = APRSListener.INSTANCE;

            // APRS-IS client
            APRSISClient client = APRSISClient.INSTANCE;

            // MQTT for packet uploads
            MQTTClient mqttClient = new MQTTClient();
            mqttClient.start();

        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            System.exit(1);
        }
    }


    public void createServices() {
        // Create services
        serviceList.clear();
        boolean pmsEnabled = configuration.getConfig(Conf.pmsEnabled, Conf.pmsEnabled.boolDefault());
        if (pmsEnabled) {
            serviceList.add(new PMSService("PMS", getMyCallNoSSID() + configuration.getConfig(Conf.pmsSSID, Conf.pmsSSID.stringDefault())));
        }

        // Net/ROM
        boolean netROMEnabled = configuration.getConfig(Conf.netromEnabled, Conf.netromEnabled.boolDefault());
        if (netROMEnabled) {
            serviceList.add(new NetROMService("NETROM", getMyCallNoSSID()+configuration.getConfig(Conf.netromSSID, Conf.netromSSID.stringDefault())));
        }
    }

    /**
     * If we are running in terminal mode, then this will be trus
     *
     * @return false for GUI mode, true for terminal mode
     */
    public boolean isTerminalMode() {
        return terminalMode;
    }


    public void initTerminalMode(Class terminalType) {
        INSTANCE = KISSet.this;

        // Redirect stdout/err
        stdOut = System.out;
        stdIn = System.in;

//        // We need to intercept the stdout for our buffer.
//        System.setOut(new PrintStream(new OutputStream() {
//            @Override
//            public void write(int b) throws IOException {
//                dataBuffer.put((byte) b);
//                stdOut.write(b);
//            }
//        }));

        // Force the logs to a null output
        System.setErr(new PrintStream(PrintStream.nullOutputStream()));
        System.setOut(new PrintStream(PrintStream.nullOutputStream()));
        initAll();

        // Instantiate the chosen defaultTerminal via reflection

        Constructor constructor = null;
        try {
            constructor = terminalType.getConstructor(InputStream.class, OutputStream.class);
            terminal = (StdTerminal) constructor.newInstance(stdIn, stdOut);
            terminal.start();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            System.exit(1);
        }

        // Text output from the TNC host to the GUI
        inpis = new PipedIOStream();
        OutputStream inpos = inpis.getOutputStream();

        // TNC hosts provides host functions that emulate a TNC
        tncHost = new TNCHost(new TerminalHost() {
            @Override
            public Object getTerminal() {
                return terminal;
            }

            @Override
            public void setTerminal(Object newTerminal) {
                // Stop the old terminal
                terminal.stop();

                // Start the new terminal
                terminal = (StdTerminal) newTerminal;
                terminal.setIOStreams(stdIn, stdOut);
                terminal.start();
                tncHost.setNewInputStream(terminal.getInputStream());

                // Pre populate the terminal with our data 'frame' buffer
                byte[] data = dataBuffer.getBytes();
                for (byte b : data) {
                    try {
                        terminal.getOutputStream().write(b & 0xFF);
                    } catch (IOException e) {
                        LOG.debug(e.getMessage(), e);
                    }
                }

            }

            @Override
            public void setStatus(String statusText, int currentStream) {
            }
        }, terminal.getInputStream(), inpos);
        tncHost.setLocalEcho(false);

        // Intercept the incoming data to the terminal and feed it to the TNC host, storing it in our buffer as well.
        Tools.runOnThread(() -> {
            try {
                while (true) {
                    int b = inpis.read();
                    if (b == -1) {
                        break;
                    }
                    dataBuffer.put((byte) b);
                    terminal.getOutputStream().write(b);
                }
            } catch (Exception e) {
                LOG.debug(e.getMessage(), e);
            }
        });

        // Catch ctrl-c and set the TNC to command mode. This uses the sun.misc.Signal class which is deprecated, but
        // there is no other way to do this at present without relying on someone else to compile some JNI for each platform
        // (then keep it up-to-date forever)
        Signal.handle(new Signal("INT"),  // SIGINT
                signal -> tncHost.setMode(Mode.CMD, true));
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public String getMyCall() {
        return myCall;
    }

    public void setMyCall(String myCall) {
        this.myCall = myCall.toUpperCase(Locale.ENGLISH);
    }

    public String getMyCallNoSSID() {
        if (!myCall.contains("-")) {
            return myCall;
        }
        return myCall.substring(0, myCall.indexOf('-'));
    }

    public Config getConfig() {
        return configuration;
    }

    public InterfaceHandler getInterfaceHandler() {
        return interfaceHandler;
    }

    public Storage getStorage() {
        return storage;
    }

    /**
     * Time to shut down
     */
    public void quit() {
        System.exit(0);
    }

    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {
        initAll();
    }

    public String getVersion() {
        String versionInfo = "Unknown/Development";

        try {
            versionInfo = new Scanner(KISSet.class.getResourceAsStream("/version.txt"), StandardCharsets.UTF_8).useDelimiter("\\A").next();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
        return versionInfo;
    }

    /**
     * Returns the capabilities of the station
     * A = ANSI colours
     * B = BBS
     * C = Deflate Compression support
     * P = PMS
     * Y = Google Brotli Compression support
     * Z = Escape sequences for next block
     *
     * @return
     */
    public String getStationCapabilities() {
        StringBuilder sb = new StringBuilder();
        // We suppoer ANSI (A), Deflate(C) and Deflate+Huffman(Z)
        sb.append("ACZ");

        if (configuration.getConfig(Conf.pmsEnabled, Conf.pmsEnabled.boolDefault())) {
            sb.append("P");
        }
        return sb.toString();
    }

    public List<Service> getServices() {
        return serviceList;
    }

}