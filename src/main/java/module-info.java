module org.prowl.kisset {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.jfree.fxgraphics2d;
    requires javafx.swing;
    requires java.logging;
    requires commons.logging;
    requires commons.configuration;
    requires java.prefs;
    requires org.reflections;
    requires com.fazecast.jSerialComm;
    requires commons.lang;

    opens org.prowl.kisset to javafx.fxml;
    exports org.prowl.kisset;
    exports org.prowl.kisset.fx;
    opens org.prowl.kisset.fx to javafx.fxml;
}