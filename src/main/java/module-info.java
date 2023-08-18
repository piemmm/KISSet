module org.prowl.kisset {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires javafx.swing;
    requires java.logging;
    requires commons.logging;
    requires commons.configuration;
    requires java.prefs;
    requires org.reflections;
    requires com.fazecast.jSerialComm;
    requires commons.lang;
    requires atlantafx.base;
    requires java.sql;
    requires nsmenufx;
    requires com.jthemedetector;
    requires com.github.oshi;
    requires versioncompare;
    requires com.google.common;
    requires aprslib;
    requires javafx.web;


    opens org.prowl.kisset to javafx.fxml;
    exports org.prowl.kisset.eventbus.events;
    exports org.prowl.kisset;
    exports org.prowl.kisset.fx;
    exports org.prowl.kisset.statistics.types;
    exports org.prowl.kisset.netrom;
    exports org.prowl.kisset.comms.host;
    exports org.prowl.kisset.comms.remote.pms;
    opens org.prowl.kisset.fx to javafx.fxml;
    exports org.prowl.kisset.objects.netrom;
}