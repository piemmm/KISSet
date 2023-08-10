package org.prowl.kisset;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Resources;
import com.jthemedetecor.OsThemeDetector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.fx.AboutController;
import org.prowl.kisset.fx.KISSetController;
import org.prowl.kisset.fx.MonitorController;
import org.prowl.kisset.fx.PreferencesController;
import org.prowl.kisset.io.InterfaceHandler;
import org.prowl.kisset.netrom.RoutingListener;
import org.prowl.kisset.statistics.Statistics;

import java.awt.*;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.AppReopenedEvent;
import java.awt.desktop.AppReopenedListener;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

public class KISSet extends Application {

    private static final Log LOG = LogFactory.getLog("KISSet");
    public static KISSet INSTANCE;
    public String myCall = "";
    private Config configuration;
    private InterfaceHandler interfaceHandler;
    private Statistics statistics;


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
        stage.show();
        controller.setup();

        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                stage.hide();
            }
        });

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
    }

    public void initAll() {
        try {

            // Statistics (heard, etc)
            statistics = new Statistics();

            // Stop any currenty running interfaces if this is a reload of the config
            if (interfaceHandler != null) {
                interfaceHandler.stop();
            }

            // Load configuration and initialise everything needed.
            configuration = new Config();

            // Set our callsign
            myCall = configuration.getConfig(Conf.callsign, Conf.callsign.stringDefault()).toUpperCase(Locale.ENGLISH);

            // Init interfaces
            interfaceHandler = new InterfaceHandler(configuration.getConfig("interfaces"));

            // Start interfaces
            interfaceHandler.start();

            // Start listening for route broadcasts
            RoutingListener routingListener = RoutingListener.INSTANCE;

        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            System.exit(1);
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

    public void showMonitor() {
        try {
            // Create the GUI.
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/MonitorController.fxml"));
            Parent root = fxmlLoader.load();
            MonitorController controller = fxmlLoader.getController();
            Scene scene = new Scene(root, 800, 280);
            stage.setTitle("Packet Monitor");
            stage.setScene(scene);
            stage.show();
            controller.setup();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
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
//            File outputFile = File.createTempFile("debug", ".log", getFileSystemView().getDefaultDirectory());
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

    public Config getConfig() {
        return configuration;
    }

    public InterfaceHandler getInterfaceHandler() {
        return interfaceHandler;
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
}