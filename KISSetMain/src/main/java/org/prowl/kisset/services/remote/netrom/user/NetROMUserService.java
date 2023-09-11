package org.prowl.kisset.services.remote.netrom.user;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.user.User;
import org.prowl.kisset.services.Service;
import org.prowl.kisset.services.remote.pms.PMSClientHandler;

import java.io.InputStream;
import java.io.OutputStream;

public class NetROMUserService extends Service {

    private static final Log LOG = LogFactory.getLog("NetROMUserService");

    private boolean stop;

    private HierarchicalConfiguration config;

    private final String callsign;

    public NetROMUserService(String name, String callsign) {
        super(name);
        LOG.debug("Starting Net/ROM Service, listening as " + callsign);
        this.callsign = callsign;
    }

    @Override
    public void start() {

    }


    public void stop() {
        stop = true;
    }


    @Override
    public String getCallsign() {
        return callsign;
    }

    @Override
    public void acceptedConnection(Interface anInterface, User user, InputStream in, OutputStream out) {

        // Just for testing
        PMSClientHandler client = new PMSClientHandler(anInterface, user, in, out);
        client.start();
    }
}