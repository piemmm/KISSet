package org.prowl.kisset;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.google.common.eventbus.Subscribe;
import com.jthemedetecor.OsThemeDetector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.services.Service;
import org.prowl.kisset.services.remote.pms.PMSService;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.fx.*;
import org.prowl.kisset.io.InterfaceHandler;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.protocols.RoutingListener;
import org.prowl.kisset.protocols.aprs.APRSISClient;
import org.prowl.kisset.protocols.aprs.APRSListener;
import org.prowl.kisset.protocols.dxcluster.DXListener;
import org.prowl.kisset.protocols.mqtt.MQTTClient;
import org.prowl.kisset.statistics.Statistics;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.AppReopenedEvent;
import java.awt.desktop.AppReopenedListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class KISSet extends Application {

    private static final Log LOG = LogFactory.getLog("KISSet");
    public static KISSet INSTANCE;
    public String myCall = "";
    private Config configuration;
    private InterfaceHandler interfaceHandler;
    private Statistics statistics;
    private Stage monitorStage;
    private Stage dxStage;
    private Stage fbbStage;
    private Stage aprsStage;
    private Storage storage;
    protected List<Service> serviceList = Collections.synchronizedList(new ArrayList<>());


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        SingleThreadBus.INSTANCE.register(this);
        Platform.setImplicitExit(false);
        try {

            // Find out if the system theme is a 'dark' or a 'light' theme.
            final OsThemeDetector detector = OsThemeDetector.getDetector();
            detector.registerListener(isDark -> {
                Platform.runLater(() -> {
                    if (isDark) {
                        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
                    } else {
                        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                    }
                });
            });
            final boolean isDarkThemeUsed = detector.isDark();
            if (isDarkThemeUsed) {
                Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
            } else {
                Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            }

            // Init resource bundles.
            Messages.init();

            initAll();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            System.exit(1);
        }

        // Create the GUI.
        FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/KISSetController.fxml"));
        Parent root = fxmlLoader.load();
        KISSetController controller = fxmlLoader.getController();
        Scene scene = new Scene(root, 750, 480);
        stage.setTitle("KISSet");
        stage.setScene(scene);
        stage.setOpacity(1 - (configuration.getConfig(Conf.terminalTransparency, Conf.terminalTransparency.intDefault()) / 100.0));

        stage.show();
        controller.setup();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                stage.hide();
            }
        });

        // Set the window icon
        stage.getIcons().add(new Image(KISSet.class.getResourceAsStream("img/icon.png")));

        // Set the taskbar icon if the OS supports it
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar task = Taskbar.getTaskbar();
                if (task.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    Toolkit toolkit = Toolkit.getDefaultToolkit();
                    java.awt.Image icon = toolkit.getImage(getClass().getResource("img/icon.png"));
                    task.setIconImage(icon);
                }
            }
        } catch (SecurityException e) {
            LOG.debug(e.getMessage(), e);
        }


        // Show the main window when the dock icon is clicked.
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().addAppEventListener(new AppReopenedListener() {
                @Override
                public void appReopened(AppReopenedEvent e) {
                    Platform.runLater(() -> {
                        stage.show();
                    });
                }
            });
        }
        createTrayIcon(stage);
    }

    public void createTrayIcon(Stage stage) {
        try {
            if (SystemTray.isSupported()) {
                Toolkit toolkit = Toolkit.getDefaultToolkit();
                java.awt.Image icon = new ImageIcon(getClass().getResource("img/tray-white.png")).getImage();
                //toolkit.getImage(getClass().getResource("img/icon.png"));
                SystemTray tray = SystemTray.getSystemTray();
                TrayIcon trayIcon = new TrayIcon(icon);
                trayIcon.setImageAutoSize(true);

                // Simple click listener
                trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        Platform.runLater(() -> {
                            stage.show();
                        });
                    }
                });

                // We can change and use this for menu popups another time.
                trayIcon.addActionListener(e -> {
                    Platform.runLater(() -> {
                        stage.show();
                    });
                });

                // Add the icon to the system tray
                tray.add(trayIcon);
            }
        } catch (Throwable e) {
            LOG.debug(e.getMessage(), e);
        }

    }

    public void initAll() {
        try {

            // This will always be desktop=
            System.setProperty("javafx.platform", "Desktop");

            // Statistics (heard, etc)
            statistics = new Statistics();

            // Stop any currenty running interfaces if this is a reload of the config
            if (interfaceHandler != null) {
                interfaceHandler.stop();
            }

            // Load configuration and initialise everything needed.
            configuration = new Config();

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

            MQTTClient mqttClient = new MQTTClient();
            mqttClient.start();


            initMonitor();
            initDX();
            initFBB();
            initAPRS();
          //  testConnectionTerminal();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            System.exit(1);
        }
    }

//    public void testConnectionTerminal() {
//        try {
//            Stage terminalStage = new Stage();
//            FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/TerminalController.fxml"));
//            Parent root = fxmlLoader.load();
//            TerminalController controller = fxmlLoader.getController();
//            Scene scene = new Scene(root, 700, 580);
//            terminalStage.setTitle("Connection test");
//            terminalStage.setScene(scene);
//            terminalStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
//                @Override
//                public void handle(WindowEvent t) {
//                    SingleThreadBus.INSTANCE.unregister(controller);
//                    terminalStage.close();
//                }
//            });
//            terminalStage.setOpacity(1 - (configuration.getConfig(Conf.monitorTransparency, Conf.monitorTransparency.intDefault()) / 100.0));
//            // This is an unfortunate hack for layout issues.
//            terminalStage.show();
//            controller.setup();
//
//
//
//
////
//            Socket s = new Socket(InetAddress.getByName("glasstty.com"), 6502);
//            controller.setConnection(s.getInputStream(), s.getOutputStream());
//           controller.start();
//        } catch (Throwable e) {
//            LOG.error(e.getMessage(), e);
//        }
//    }

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
            //serviceList.add(new NetROMService("NETROM", getMyCallNoSSID()+configuration.getConfig(Conf.netromSSID, Conf.netromSSID.stringDefault())));
        }
    }

    public void showPreferences() {
        try {
            // Create the GUI.
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/PreferencesController.fxml"));
            Parent root = fxmlLoader.load();
            PreferencesController controller = fxmlLoader.getController();
            Scene scene = new Scene(root, 640, 480);
            stage.setTitle("Preferences");
            stage.setScene(scene);
            stage.show();
            controller.setup();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void initMonitor() {
        try {
            if (monitorStage == null) {
                monitorStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/MonitorController.fxml"));
                Parent root = fxmlLoader.load();
                MonitorController controller = fxmlLoader.getController();
                Scene scene = new Scene(root, 800, 280);
                monitorStage.setTitle("Packet Monitor");
                monitorStage.setScene(scene);
                monitorStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent t) {
                        monitorStage.close();
                    }
                });
                monitorStage.setOpacity(1 - (configuration.getConfig(Conf.monitorTransparency, Conf.monitorTransparency.intDefault()) / 100.0));


                controller.setup();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void showMonitor() {
        monitorStage.show();
    }

    public void initDX() {
        try {
            if (dxStage == null) {
                dxStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/DXController.fxml"));
                Parent root = fxmlLoader.load();
                DXController controller = fxmlLoader.getController();
                Scene scene = new Scene(root, 800, 280);
                dxStage.setTitle("DX Monitor");
                dxStage.setScene(scene);
                dxStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent t) {
                        dxStage.close();
                    }
                });
                dxStage.setOpacity(1 - (configuration.getConfig(Conf.dxTransparency, Conf.dxTransparency.intDefault()) / 100.0));


                controller.setup();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void showDX() {
        dxStage.show();
    }

    public void initFBB() {
        try {
            if (fbbStage == null) {
                fbbStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/FBBController.fxml"));
                Parent root = fxmlLoader.load();
                FBBController controller = fxmlLoader.getController();
                Scene scene = new Scene(root, 800, 280);
                fbbStage.setTitle("Broadcast FBB Messages");
                fbbStage.setScene(scene);
                fbbStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent t) {
                        fbbStage.close();
                    }
                });
                fbbStage.setOpacity(1 - (configuration.getConfig(Conf.dxTransparency, Conf.dxTransparency.intDefault()) / 100.0));


                controller.setup();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void showFBB() {
        fbbStage.show();
    }

    public void initAPRS() {
        try {
            if (aprsStage == null) {
                aprsStage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/APRSController.fxml"));
                Parent root = fxmlLoader.load();
                APRSController controller = fxmlLoader.getController();
                Scene scene = new Scene(root, 640, 640);
                aprsStage.setTitle("APRS Map Viewer");
                aprsStage.setScene(scene);
                aprsStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent t) {
                        aprsStage.close();
                    }
                });


                controller.setup();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void showAPRS() {
        aprsStage.show();
    }


    public void showAbout() {
        try {
            // Create the GUI.
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/AboutController.fxml"));
            Parent root = fxmlLoader.load();
            AboutController controller = fxmlLoader.getController();
            Scene scene = new Scene(root, 350, 380);
            stage.setTitle("About KISSet");
            stage.setScene(scene);
            stage.show();
            controller.setup();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void init() throws Exception {
        super.init();
        INSTANCE = KISSet.this;
        // Push debugging to a file if we are debugging a built app with no console
//        try {
//            File outputFile = File.createTempFile("debug", ".log", FileSystemView.getFileSystemView().getDefaultDirectory());
//            PrintStream output = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)), true);
//            System.setOut(output);
//            System.setErr(output);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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
        Platform.exit();
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
     * C = Compression
     * P = PMS
     * Z = Escape sequences for next block
     *
     * @return
     */
    public String getStationCapabilities() {
        return "APC";
    }

    public List<Service> getServices() {
        return serviceList;
    }

    ;

}