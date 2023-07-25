package org.prowl.kisset;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.fx.KISSetController;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.io.tcp.KISSviaTCP;

import java.io.IOException;
import java.util.Locale;

public class KISSet extends Application {

    public static KISSet INSTANCE;

    private static final Log LOG = LogFactory.getLog("KISSet");

    private Config configuration;

    public String myCall = "N0CALL";

    @Override
    public void start(Stage stage) throws IOException {

        try {
            // Init resource bundles.
            Messages.init();

            // Load configuration and initialise everything needed.
            configuration = new Config();

            // Set our callsign
            myCall = configuration.getConfig("callsign", "NOCALL").toUpperCase(Locale.ENGLISH);

            // Create the GUI


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
        FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/kissetController.fxml"));
        Parent root = fxmlLoader.load();
        KISSetController controller = fxmlLoader.getController();
        Scene scene = new Scene(root, 640, 480);
        stage.setTitle("KISSet - TH-D74(Bluetooth)");
        stage.setScene(scene);
        stage.show();
        controller.setup();

    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void init() throws Exception {
        super.init();
        INSTANCE = KISSet.this;

    }


    public String getMyCall() {
        return myCall;
    }


    /**
     * Time to shut down
     */
    public void quit() {
        System.exit(0);
    }
}