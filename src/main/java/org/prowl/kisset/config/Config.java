package org.prowl.kisset.config;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

public class Config {


    private static final Log LOG = LogFactory.getLog("Config");

    private static final String CONFIG_FILE = "config.xml";

    private XMLConfiguration configuration;

    public Config() {
        loadConfig();
    }

    public void loadConfig() {
        LOG.info("Loading configuration from: " + new File(new File("").getAbsolutePath(), CONFIG_FILE));
        File configFile = new File(new File("").getAbsolutePath(), CONFIG_FILE);
        try {
            configuration = new XMLConfiguration(configFile);
        } catch (Throwable e) {
            if (configFile.exists()) {
                e.printStackTrace();
                System.err.println("Search path: " + new File("").getAbsolutePath());
                System.err.println("Unable to load production config file, exiting:" + e.getMessage());
                System.exit(1);
            } else {
                // Does not exist, make a default config
                configuration = makeDefaultConfig();
            }
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

    public void setProperty(String name, Object value) {
        configuration.setProperty(name, value);
    }

    public void saveConfig() {
        LOG.info("Saving configuration to: " + new File(new File("").getAbsolutePath(), CONFIG_FILE));
        try {
            configuration.save();
        } catch (Throwable e) {
            LOG.error("Unable to save configuration file: " + e.getMessage(), e);
        }
    }

    private XMLConfiguration makeDefaultConfig() {
        try {
            XMLConfiguration config = new XMLConfiguration(Config.class.getResource("config-default.xml"));
            return config;
        } catch(ConfigurationException e) {
            LOG.error(e.getMessage(),e);
        }
        return null;
    }

}
