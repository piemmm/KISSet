package org.prowl.kisset.services.host.parser.commands;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangeCompleteEvent;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.io.KISSviaSerial;
import org.prowl.kisset.io.KISSviaTCP;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@TNCCommand
/**
 * Allows the user to configure KISS interfaces from the command line
 */
public class DeConfigure extends Command {
    private static final Log LOG = LogFactory.getLog("DeConfigure");

    public static final Mode DECONFIGURE_INTERFACE = new Mode("DECONFIGURE_INTERFACE");

    // The current state of the message builder.
    private DeConfigure.State configureState;

    private ConfigurationEntry configurationEntry;

    @Override
    public boolean doCommand(String[] data) throws IOException {


        Mode mode = getMode();
        LOG.debug("deconfigureinterface:" + mode + ":" + configureState + ":" + data[0]);

        if (mode.equals(Mode.CMD) && data[0].length() > 0) {
            // Put into send message mode
            configureState = null;
            setMode(DECONFIGURE_INTERFACE);
            configureInterface(data);
            return true;
        } else if (mode.equals(DECONFIGURE_INTERFACE)) {
            configureInterface(data);
            return true;
        }
        return false;
    }

    /**
     * Build the message from the data provided.
     *
     * @param data The data to build the message from.
     */
    public void configureInterface(String[] data) throws IOException {

        /**
         * The user has confirmed the configuration
         */
        if (State.START.equals(configureState)) {
            int interfaceNumber = Tools.getInteger(data[0], -1);
            if (interfaceNumber < 0  || interfaceNumber > KISSet.INSTANCE.getInterfaceHandler().getInterfaces().size() - 1) {
                writeToTerminal(ANSI.RED + "*** Invalid interface number" + CR);
                configureState = null;
                setMode(Mode.CMD);
                return;
            } else {
                removeInterface(interfaceNumber);
                configureState = null;
                setMode(Mode.CMD);
                writeToTerminal(ANSI.GREEN + "*** Interface deleted" + CR);
                return;
            }
        }

        if (configureState == null) {
            configurationEntry = new ConfigurationEntry();
            configureState = State.START;
            writeToTerminal(ANSI.YELLOW+"*** "+ ANSI.UNDERLINE+"Interface Configuration"+ANSI.NORMAL+CR);
            writeToTerminal(ANSI.WHITE+" Abort interface deletion by pressing ctrl-c at any time" + CR);
            writeToTerminal(" Choose a interface from the following list: "+CR);
            writeToTerminal(ANSI.CYAN);
            showInterfaces();
            writeToTerminal(ANSI.NORMAL+"Enter the number of the driver to delete: ");
        }
    }

    /**
     * List the current interfaces to the user
     */
    public void showInterfaces() throws IOException {
        ChangeInterface.showInterfaces(this);
    }


    /**
     * Add the interface section to our configuration
     */
    public void removeInterface(int interfaceNumber) {
        Config config = KISSet.INSTANCE.getConfig();
        // Get the UUID of the interface to remove
        String uuid = KISSet.INSTANCE.getInterfaceHandler().getInterfaces().get(interfaceNumber).getUUID();
        HierarchicalConfiguration interfacesNode = config.getConfig("interfaces");
        // Get a list of all interfaces
        List<HierarchicalConfiguration> interfaceList = interfacesNode.configurationsAt("interface");
        // Get the one with the correct UUID
        for (HierarchicalConfiguration interfaceNode : interfaceList) {
            if (interfaceNode.getString("uuid").equals(uuid)) {
                // Remove the interface node from the interfaces node.
                config.getConfig("interfaces").getRootNode().removeChild(interfaceNode.getRootNode());
                break;
            }
        }
        config.saveConfig();

        // Tell everything the config just changed
        SingleThreadBus.INSTANCE.post(new ConfigurationChangedEvent());
        SingleThreadBus.INSTANCE.post(new ConfigurationChangeCompleteEvent(true)); // Sent after the previous dispatch

    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"deconf", "deconfigure", "deconfig"};
    }


    private enum State {
        START,
        CONFIRM,
        END;
    }

    private class ConfigurationEntry {


        public int interfaceNumber;

        private ConfigurationEntry() {}

    }

}
