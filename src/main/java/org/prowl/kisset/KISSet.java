package org.prowl.kisset;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.fx.KISSetController;
import org.prowl.kisset.fx.PreferencesController;
import org.prowl.kisset.io.InterfaceHandler;

import java.io.IOException;
import java.util.Locale;

public class KISSet extends Application {

    private static final Log LOG = LogFactory.getLog("KISSet");
    public static KISSet INSTANCE;
    public String myCall = "N0CALL";
    private Config configuration;
    private InterfaceHandler interfaceHandler;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        try {
            // Init resource bundles.
            Messages.init();

            initAll();
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            System.exit(1);
        }

        // All done
        Thread t = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();


        // Create the GUI.
        FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/KISSetController.fxml"));
        Parent root = fxmlLoader.load();
        KISSetController controller = fxmlLoader.getController();
        Scene scene = new Scene(root, 700, 480);
        stage.setTitle("KISSet - TH-D74(Bluetooth)");
        stage.setScene(scene);
        stage.show();
        controller.setup();

    }

    public void initAll() {
        try {

            // Stop any currenty running interfaces if this is a reload of the config
            if (interfaceHandler != null) {
                interfaceHandler.stop();
            }

            // Load configuration and initialise everything needed.
            configuration = new Config();

            // Set our callsign
            myCall = configuration.getConfig("callsign", "NOCALL").toUpperCase(Locale.ENGLISH);

            // Init interfaces
            interfaceHandler = new InterfaceHandler(configuration.getConfig("interfaces"));

            // Start interfaces
            interfaceHandler.start();

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

    @Override
    public void init() throws Exception {
        super.init();
        INSTANCE = KISSet.this;

    }


    public String getMyCall() {
        return myCall;
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
        System.exit(0);
    }
}