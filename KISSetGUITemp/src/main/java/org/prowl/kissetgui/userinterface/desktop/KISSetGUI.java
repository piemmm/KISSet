package org.prowl.kissetgui.userinterface.desktop;

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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.services.Service;
import org.prowl.kisset.services.remote.pms.PMSService;
import org.prowl.kissetgui.userinterface.desktop.fx.*;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.AppReopenedEvent;
import java.awt.desktop.AppReopenedListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.*;

public class KISSetGUI extends Application {


    private static Log LOG;


    public static KISSetGUI INSTANCE;
    public String myCall = "";
    protected List<Service> serviceList = Collections.synchronizedList(new ArrayList<>());
    private Config configuration = KISSet.INSTANCE.getConfig();
    private Stage monitorStage;
    private Stage dxStage;
    private Stage fbbStage;
    private Stage aprsStage;


    /**
     * If this is true, then we are running in a terminal
     */
    public static boolean terminalMode = false;

    public KISSetGUI() {
        super();
    }

    public static void main(String[] args) {
                KISSet kisset = new KISSet();
                kisset.initAll();

         launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        SingleThreadBus.INSTANCE.register(this);
        INSTANCE = this;
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


            initAll();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            System.exit(1);
        }

        // Create the GUI.
        FXMLLoader fxmlLoader = new FXMLLoader(KISSetGUI.class.getResource("fx/KISSetController.fxml"));
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
        stage.getIcons().add(new Image(KISSetGUI.class.getResourceAsStream("img/icon.png")));

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

            if (!terminalMode) {
                initMonitor();
                initDX();
                initFBB();
                initAPRS();
            }
            //  testConnectionTerminal();
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
            //serviceList.add(new NetROMService("NETROM", getMyCallNoSSID()+configuration.getConfig(Conf.netromSSID, Conf.netromSSID.stringDefault())));
        }
    }

    public void showPreferences() {
        try {
            // Create the GUI.
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(KISSetGUI.class.getResource("fx/PreferencesController.fxml"));
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
                FXMLLoader fxmlLoader = new FXMLLoader(KISSetGUI.class.getResource("fx/MonitorController.fxml"));
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
                FXMLLoader fxmlLoader = new FXMLLoader(KISSetGUI.class.getResource("fx/DXController.fxml"));
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
                FXMLLoader fxmlLoader = new FXMLLoader(KISSetGUI.class.getResource("fx/FBBController.fxml"));
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
                FXMLLoader fxmlLoader = new FXMLLoader(KISSetGUI.class.getResource("fx/APRSController.fxml"));
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
            FXMLLoader fxmlLoader = new FXMLLoader(KISSetGUI.class.getResource("fx/AboutController.fxml"));
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
        INSTANCE = KISSetGUI.this;
        LOG = LogFactory.getLog("KISSet");
        super.init();


        // Use bouncycastle for crypto
        Security.addProvider(new BouncyCastleProvider());
        // There is an issue with x25519 keys on some systems, so we disable them for the moment.
        System.setProperty("jdk.tls.namedGroups", "secp256r1, secp384r1, secp521r1, ffdhe2048, ffdhe3072, ffdhe4096, ffdhe6144, ffdhe8192");

//        // Push debugging to a file if we are debugging a built app with no console
//        if (!terminalMode) {
//        try {
//            File outputFile = File.createTempFile("debug", ".log", FileSystemView.getFileSystemView().getDefaultDirectory());
//            PrintStream output = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)), true);
//            System.setOut(output);
//            System.setErr(output);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        }
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
            versionInfo = new Scanner(KISSetGUI.class.getResourceAsStream("/version.txt"), StandardCharsets.UTF_8).useDelimiter("\\A").next();
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

}