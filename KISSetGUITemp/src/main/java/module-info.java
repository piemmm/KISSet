module org.prowl.kissetgui {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires javafx.swing;

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
    requires org.eclipse.paho.client.mqttv3;
    requires com.google.errorprone.annotations;
    requires org.bouncycastle.provider;
    requires jdk.unsupported;
    requires com.googlecode.lanterna;
    requires org.prowl.kisset;



    exports org.prowl.maps;
    exports org.prowl.kissetgui.userinterface.desktop.fx;
    opens org.prowl.kissetgui.userinterface.desktop.fx to javafx.fxml;
    exports org.prowl.kissetgui.userinterface.desktop;
    opens org.prowl.kissetgui.userinterface.desktop to javafx.fxml;
    exports org.prowl.kissetgui.userinterface.desktop.utils;
    opens org.prowl.kissetgui.userinterface.desktop.utils to javafx.fxml;

}

