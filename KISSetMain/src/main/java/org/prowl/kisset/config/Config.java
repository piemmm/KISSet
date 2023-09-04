package org.prowl.kisset.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.Writer;
import java.util.List;

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

        // Load the configuration from disk - overloaded to stop a commons config bug with blank lines appearing
        // in the saved config file. The xslt transform nukes them.
        try {
            configuration = new XMLConfiguration() {
               public void save(Writer writer) throws ConfigurationException
                {
                    try
                    {
                        Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer(new StreamSource(Config.class.getResourceAsStream("tidy.xslt")));//createTransformer();
                        Source source = new DOMSource(createDocument());
                        Result result = new StreamResult(writer);
                        transformer.transform(source, result);
                    }
                    catch (TransformerException e)
                    {
                        throw new ConfigurationException("Unable to save the configuration", e);
                    }
                    catch (TransformerFactoryConfigurationError e)
                    {
                        throw new ConfigurationException("Unable to save the configuration", e);
                    }
                }
            };

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

    /**
     * Convenience method to get an interface given a uuid
     */
    public HierarchicalConfiguration getInterfaceConfig(String uuid) {
        // Get the UUID of the interface to remove
        HierarchicalConfiguration interfacesNode = getConfig("interfaces");
        // Get a list of all interfaces
        List<HierarchicalConfiguration> interfaceList = interfacesNode.configurationsAt("interface");
        // Get the one with the correct UUID
        for (HierarchicalConfiguration interfaceNode : interfaceList) {
            if (interfaceNode.getString("uuid").equals(uuid)) {
                return interfaceNode;
            }
        }

        // Interface was not found
        return null;
    }

}
