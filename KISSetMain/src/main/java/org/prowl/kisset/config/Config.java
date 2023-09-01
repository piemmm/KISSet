package org.prowl.kisset.config;

import org.apache.commons.configuration.ConfigurationException;
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
        loadConfig();
    }

    public File getConfigFile() {
        String userHome = System.getProperty("user.home");
        File appDir = new File(userHome, ".kisset");
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        File configFile = new File(appDir, CONFIG_FILE);
        return configFile;
    }

    public void loadConfig() {
        LOG.info("Loading configuration from: " + getConfigFile());
        File configFile = getConfigFile();
        // No config? Use the default one
        if (!configFile.exists()) {
            // Does not exist, make a default config
            makeDefaultConfig();
            return;
        }

        // Load the configuration from disk
        try {
            configuration = new XMLConfiguration();
            configuration.setDelimiterParsingDisabled(true);
            configuration.load(configFile);
        } catch (Throwable e) {

            e.printStackTrace();
            System.err.println("Search path: " + new File("").getAbsolutePath());
            System.err.println("Unable to load production config file, exiting:" + e.getMessage());
            System.exit(1);

        }
    }

    public String getConfig(Conf name, String defaultVal) {
        return configuration.getString(name.name(), defaultVal);
    }

    public String getConfig(String name, String defaultVal) {
        return configuration.getString(name, defaultVal);
    }

    public int getConfig(Conf name, int defaultVal) {
        return configuration.getInt(name.name(), defaultVal);
    }

    public boolean getConfig(Conf name, boolean defaultValue) {
        return configuration.getBoolean(name.name(), defaultValue);
    }

    public SubnodeConfiguration getConfig(String subNode) {
        return configuration.configurationAt(subNode);
    }

    public Config setProperty(Conf name, Object value) {
        configuration.setProperty(name.name(), value);
        return this;
    }

    public Config setProperty(String name, Object value) {
        configuration.setProperty(name, value);
        return this;
    }

    public void saveConfig() {
        File configFile = getConfigFile();
        LOG.info("Saving configuration to: " + configFile);
        try {
            configuration.save(configFile);
        } catch (Throwable e) {
            LOG.error("Unable to save configuration file: " + e.getMessage(), e);
        }
    }

    private void makeDefaultConfig() {
        LOG.info("Making new default configuration");
        try {
            configuration = new XMLConfiguration(Config.class.getResource("config-default.xml"));
            saveConfig();
        } catch (ConfigurationException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
