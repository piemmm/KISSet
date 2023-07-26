package org.prowl.kisset.io;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InterfaceHandler {

    private static final Log LOG = LogFactory.getLog("InterfaceHandler");

    private SubnodeConfiguration configuration;

    private List<Interface> interfaces = new ArrayList<>();


    public InterfaceHandler(SubnodeConfiguration configuration) throws IOException {
        this.configuration = configuration;
        parseConfiguration();
    }

    /**
     * Parse the configuration and setup the interface
     */
    public void parseConfiguration() {
        // Get a list of connectors from the config file
        List<HierarchicalConfiguration> interfaceConfigurations = configuration.configurationsAt("interface");

        // Go create and configure each one.
        for (HierarchicalConfiguration interfaceConfiguration : interfaceConfigurations) {
            String className = interfaceConfiguration.getString("className");
            Interface anInterface = null;
            try {
                anInterface = (Interface) Class.forName(className).getConstructor(HierarchicalConfiguration.class).newInstance(interfaceConfiguration);
                interfaces.add(anInterface);
                LOG.info("Added interface: " + className);
            } catch (Throwable e) {
                // Something blew up. Log it and carry on.
                LOG.error("Unable to add interface: " + className, e);
            }
        }


        // If there are no connectors configured then exit as there's little point in
        // continuing.
        if (interfaces.size() == 0) {
            LOG.error("Not starting as no interfaces have been configured");
            System.exit(1);
        }

    }

    public void start() {
        LOG.info("Starting interface...");
        for (Interface iface : interfaces) {
            try {
                LOG.info("Starting: " + iface.toString());
                iface.start();

            } catch (Throwable e) {
                LOG.error("Unable to start interface: " + iface.toString(), e);
            }
        }
    }

    /**
     * Get the interface that services the requested radio port (eg: 0=144MHz,
     * 1=433MHZ, etc)
     *
     * @param port
     * @return the port, or null if the port does not exist
     */
    public Interface getPort(int port) {
        if (port < interfaces.size()) {
            return interfaces.get(port);
        }
        return null;
    }

    /**
     * Returns a list of interfaces
     *
     * @return
     */
    public List<Interface> getPorts() {
        return new ArrayList(interfaces);
    }


}
