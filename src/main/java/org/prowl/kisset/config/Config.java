package org.prowl.kisset.config;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

public class Config {


    private static final Log LOG = LogFactory.getLog("Config");

    private static final String CONFIG_FILE = "config.xml";

    private XMLConfiguration configuration;

    public Config() {
        LOG.info("Loading configuration from: " + new File(new File("").getAbsolutePath(), CONFIG_FILE));
        try {
            configuration = new XMLConfiguration(new File(new File("").getAbsolutePath(), CONFIG_FILE));
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("Search path: " + new File("").getAbsolutePath().toString());
            System.err.println("Unable to load production config file, exiting:" + e.getMessage());
            System.exit(1);
        }
    }


    public String getConfig(String name, String defaultVal) {
        return configuration.getString(name, defaultVal);
    }

    public int getConfig(String name, int defaultVal) {
        return configuration.getInt(name, defaultVal);
    }

    public boolean getConfig(String name, boolean defaultValue) {
        return configuration.getBoolean(name, defaultValue);
    }

    public SubnodeConfiguration getConfig(String subNode) {
        return configuration.configurationAt(subNode);
    }

    public void saveConfig() {
        try {
            configuration.save();
        } catch (Throwable e) {
            LOG.error("Unable to save configuration file: " + e.getMessage(), e);
        }
    }

}
