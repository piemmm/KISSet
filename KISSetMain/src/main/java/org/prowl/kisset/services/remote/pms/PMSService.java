package org.prowl.kisset.services.remote.pms;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.user.User;
import org.prowl.kisset.services.Service;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * PMS Service
 */
public class PMSService extends Service {

    private static final Log LOG = LogFactory.getLog("PMSService");

    private boolean stop;

    private HierarchicalConfiguration config;

    private final String callsign;

    public PMSService(String name, String callsign) {
        super(name);
        LOG.debug("Starting PMS Service, listening on " + callsign);
        this.callsign = callsign;
    }

    public void acceptedConnection(Interface anInterface, User user, InputStream in, OutputStream out) {
        PMSClientHandler client = new PMSClientHandler(anInterface, user, in, out);
        client.start();
    }

    public void start() {
    }

    public void stop() {
        stop = true;
    }

    @Override
    public String getCallsign() {
        return callsign;
    }


}
