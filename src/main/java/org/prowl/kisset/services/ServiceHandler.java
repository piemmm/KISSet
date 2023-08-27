package org.prowl.kisset.services;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class ServiceHandler {

    private static final Log LOG = LogFactory.getLog("ServiceHandler");

    private final SubnodeConfiguration configuration;

    private final List<Service> services = new ArrayList<>();

    public ServiceHandler(SubnodeConfiguration configuration) {
        this.configuration = configuration;
        parseConfiguration();
    }

    public Service getServiceForName(String name) {
        for (Service service : services) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

    /**
     * Return a copy array of the current running services.
     *
     * @return
     */
    public List<Service> getServices() {
        return new ArrayList<>(services);
    }

    public void parseConfiguration() {

        // Get a list of user interfaces from the config file
        List<HierarchicalConfiguration> services = configuration.configurationsAt("service");

        // Go create and configure each one.
        for (HierarchicalConfiguration service : services) {
            String className = service.getString("type");
            try {
                Service con = (Service) Class.forName(className).getConstructor(HierarchicalConfiguration.class).newInstance(service);
                this.services.add(con);
                LOG.info("Added service: " + className);
            } catch (Throwable e) {
                // Something blew up. Log it and carry on.
                LOG.error("Unable to add service: " + className, e);
            }

        }
    }

    public void start() {
        LOG.info("Starting services...");
        for (Service service : services) {
            try {
                LOG.info("Starting service: " + service.getName());
                service.start();

            } catch (Throwable e) {
                LOG.error("Unable to start service: " + service.getName(), e);
            }
        }
    }
}