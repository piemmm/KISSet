module org.prowl.kisset {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;

    opens org.prowl.kisset to javafx.fxml;
    exports org.prowl.kisset;
    exports org.prowl.kisset.comms.fx;
    opens org.prowl.kisset.comms.fx to javafx.fxml;
}