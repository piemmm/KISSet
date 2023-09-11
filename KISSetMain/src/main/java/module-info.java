module org.prowl.kisset {

    requires java.logging;
    requires commons.logging;
    requires commons.configuration;
    requires java.prefs;
    requires org.reflections;
    requires com.fazecast.jSerialComm;
    requires commons.lang;
    requires com.github.oshi;
    requires com.google.common;
    requires aprslib;
    requires org.eclipse.paho.client.mqttv3;
    requires com.google.errorprone.annotations;
    requires org.bouncycastle.provider;
    requires jdk.unsupported;
    requires com.googlecode.lanterna;
    requires java.xml;


    exports org.prowl.ax25;
    exports org.prowl.kisset.eventbus.events;
    exports org.prowl.kisset.protocols.mqtt;
    exports org.prowl.kisset;
    exports org.prowl.kisset.objects;
    exports org.prowl.kisset.protocols.aprs;
    exports org.prowl.kisset.statistics.types;
    exports org.prowl.kisset.protocols;
    exports org.prowl.kisset.protocols.dxcluster;
    exports org.prowl.kisset.protocols.fbb;
    exports org.prowl.kisset.services.host;
    exports org.prowl.kisset.services.remote.pms;
    exports org.prowl.kisset.objects.routing;
    exports org.prowl.kisset.protocols.netrom;
    exports org.prowl.kisset.util;
    exports org.prowl.kisset.config;
    exports org.prowl.kisset.eventbus;
    exports org.prowl.kisset.services.host.parser;
    exports org.prowl.kisset.userinterface;
    exports org.prowl.kisset.annotations;
    exports org.prowl.kisset.services.host.parser.commands;
    exports org.prowl.kisset.io;
    exports org.prowl.ax25.util;
    exports org.prowl.kisset.services;
    exports org.prowl.kisset.objects.dxcluster;
    exports org.prowl.kisset.protocols.core;
    exports org.prowl.kisset.util.compression.deflate;
    exports org.prowl.kisset.util.compression.deflatehuffman;
    exports org.prowl.kisset.services.remote.netrom.user;
    exports org.prowl.kisset.services.remote.netrom.circuit;
    exports org.prowl.kisset.services.remote.netrom.server;

}

