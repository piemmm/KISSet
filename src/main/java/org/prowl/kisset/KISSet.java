package org.prowl.kisset;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class KISSet extends Application {

    public static KISSet INSTANCE;

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/kissetController.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();



    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void init() throws Exception {
        super.init();
        INSTANCE = KISSet.this;

    }

    /**
     * Time to shut down
     */
    public void quit() {
        System.exit(0);
    }
}