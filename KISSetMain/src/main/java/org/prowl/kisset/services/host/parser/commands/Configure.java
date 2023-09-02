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
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.io.KISSviaSerial;
import org.prowl.kisset.io.KISSviaTCP;
import org.prowl.kisset.objects.Priority;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.objects.messages.Message;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;

@TNCCommand
/**
 * Allows the user to configure KISS interfaces from the command line
 */
public class Configure extends Command {
    private static final Log LOG = LogFactory.getLog("Configure");

    // The current state of the message builder.
    private Configure.State configureState;

    private ConfigurationEntry configurationEntry;

    @Override
    public boolean doCommand(String[] data) throws IOException {


        Mode mode = getMode();
        LOG.debug("configureinterface:" + mode + ":" + configureState + ":" + data[0]);

        if (mode.equals(Mode.CMD) && data[0].length() > 0) {
            // Put into send message mode
            configureState = null;
            setMode(Mode.CONFIGURE_INTERFACE);
            configureInterface(data);
            return true;
        } else if (mode.equals(Mode.CONFIGURE_INTERFACE)) {
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
        Mode mode = getMode();


        /**
         * The user has confirmed the configuration
         */
        if (State.CONFIRM_CONFIGURATION.equals(configureState)) {
            if (data[0].equalsIgnoreCase("Y")) {
                // Add the interface to the configuration

                addInterface(configurationEntry);
               // config.addInterface(configurationEntry.driverClass, configurationEntry.serialPort, configurationEntry.baudRate, configurationEntry.ipAddress, configurationEntry.port);
                writeToTerminal(ANSI.GREEN+ANSI.BOLD+"*** Interface created"+ANSI.NORMAL+CR);
                setMode(Mode.CMD);
            } else {
                writeToTerminal(ANSI.RED+ANSI.BOLD+"*** Interface creation aborted"+ANSI.NORMAL+CR);
                configurationEntry = new ConfigurationEntry();
                setMode(Mode.CMD);
            }
        }

        /**
         * The user now enters the port or baud rate
         */
        if (State.GET_INTERFACE_PORT.equals(configureState)) {
            // Port supplied
            int portNumber = Tools.getInteger(data[0],-1);
            if (portNumber >= 0 && portNumber < 65536) {
                configurationEntry.port = portNumber;
                configureState = State.CONFIRM_CONFIGURATION;
                writeToTerminal("Do you wish to create this interface(Y|N)? ");
            } else {
                writeToTerminal(ANSI.RED+ANSI.BOLD+"*** Invalid port number" +ANSI.NORMAL+ CR);
            }
        } else if (State.GET_INTERFACE_BAUD.equals(configureState)) {
            // Baud rate supplied
            int baudRateSelection = Tools.getInteger(data[0],-1);
            if (baudRateSelection >= 0 && baudRateSelection < KISSviaSerial.VALID_BAUD_RATES.length) {
                configurationEntry.baudRate = ""+KISSviaSerial.VALID_BAUD_RATES[baudRateSelection];
                configureState = State.CONFIRM_CONFIGURATION;
                writeToTerminal("Do you wish to create this interface(Y|N)? ");
            } else {
                writeToTerminal(ANSI.RED+ANSI.BOLD+"*** Invalid baud rate" +ANSI.NORMAL+ CR);
            }
        }

        /**
         * The user has supplied the IP address of the interface
         */
        if (State.GET_INTERFACE_IP.equals(configureState)) {
            // IP supplied.
            String ipAddress = data[0];
            if (Tools.isValidIPorHostname(ipAddress)) {
                configurationEntry.ipAddress = ipAddress;
                configureState = State.GET_INTERFACE_PORT;
                writeToTerminal("Enter the port number to use: ");
            } else {
                writeToTerminal(ANSI.RED+ANSI.BOLD+"*** Invalid IP address" +ANSI.NORMAL+ CR);
            }
        } else if (State.GET_INTERFACE_SERIAL_PORT.equals(configureState)) {
            // Serial port supplied
            int portNumber = Tools.getInteger(data[0],-1);
            List<SerialPort> ports = KISSviaSerial.getListOfSerialPorts();
            configurationEntry.serialPort = ports.get(portNumber).getSystemPortName();
            if (portNumber >= 0 && portNumber < ports.size()) {
                configurationEntry.serialPort = ports.get(portNumber).getSystemPortName();
                configureState = State.GET_INTERFACE_BAUD;
                writeToTerminal("Enter the baud rate to use: ");
                showBaudRates();
            } else {
                writeToTerminal(ANSI.RED+ANSI.BOLD+"*** Invalid port number" +ANSI.NORMAL+ CR);
            }
        }

        if (State.START.equals(configureState)) {
            // Validate the entered number of the driver
            int driverNumber = Tools.getInteger(data[0],-1);
            if (driverNumber == 0) {
                configurationEntry.driverClass = KISSviaSerial.class.getName();
                configureState = State.GET_INTERFACE_SERIAL_PORT;
                showSerialPortList();
                writeToTerminal("Enter the serial port to use: ");
            } else if (driverNumber == 1) {
                //configurationEntry.driverClass = KISSviaSerialTNC.class.getName();
                configureState = State.GET_INTERFACE_SERIAL_PORT;
                showSerialPortList();
                writeToTerminal("Enter the serial port to use: ");
            } else if (driverNumber == 2) {
                configurationEntry.driverClass = KISSviaTCP.class.getName();
                configureState = State.GET_INTERFACE_IP;
                writeToTerminal("Enter the IP address to use: ");
            } else {
                writeToTerminal(ANSI.RED+ANSI.BOLD+"*** Invalid driver number" +ANSI.NORMAL+ CR);
            }

        }

        if (configureState == null) {
            configurationEntry = new ConfigurationEntry();
            configureState = State.START;
            writeToTerminal(ANSI.YELLOW+"*** "+ ANSI.UNDERLINE+"Interface Configuration"+ANSI.NORMAL+CR);
            writeToTerminal(ANSI.WHITE+" Abort interface creation by pressing ctrl-c at any time" + CR);
            writeToTerminal(" Choose a driver from the following list: "+CR);
            writeToTerminal(ANSI.CYAN+"  0: KISS via Serial (NinoTNC, Kenwood TH-D74, etc)"+CR);
            writeToTerminal("  1: KISS via Serial TNC (Kantronics, TNC-2, MFJ-1278, PK-232, etc)"+CR);
            writeToTerminal("  2: KISS via TCP/IP (Direwolf -p KISS mode, etc)"+ANSI.NORMAL+CR);
            writeToTerminal("Enter the number of the driver to use: ");
        }
    }

    public void showSerialPortList() throws IOException {
        List<SerialPort> ports = KISSviaSerial.getListOfSerialPorts();
        writeToTerminal("List of available serial ports:"+CR+ANSI.CYAN);
        int count = 0;
        for (SerialPort p: ports) {
            writeToTerminal(" "+count+": "+p.getSystemPortName()+CR);
            count++;
        }
        writeToTerminal(ANSI.NORMAL);
    }

    public void showBaudRates() throws IOException {
        writeToTerminal("List of available baud rates:"+CR+ANSI.CYAN);
        int count = 0;
        for (int baud: KISSviaSerial.VALID_BAUD_RATES) {
            writeToTerminal(" "+count+": "+baud+CR);
            count++;
        }
        writeToTerminal(ANSI.NORMAL);
    }

    /**
     * Add the interface section to our configuration
     * @param entry
     */
    public void addInterface(ConfigurationEntry entry) {
        Config config = KISSet.INSTANCE.getConfig();
        HierarchicalConfiguration interfacesNode = config.getConfig("interfaces");
        // Create a new interface in the interfaces list which contains many interface tags (one for each interface)
        HierarchicalConfiguration.Node node = new HierarchicalConfiguration.Node("interface");
        HierarchicalConfiguration.Node nodeUUID = new HierarchicalConfiguration.Node("uuid", java.util.UUID.randomUUID().toString());
        node.addChild(nodeUUID);
        ArrayList<HierarchicalConfiguration.Node> nodeList = new ArrayList<>();
        nodeList.add(node);
        interfacesNode.addNodes("", nodeList);
        List<HierarchicalConfiguration> iConfigs = interfacesNode.configurationsAt("interface");
        HierarchicalConfiguration interfaceNode = iConfigs.get(iConfigs.size() - 1); // This is a bit hacky, but works.
        interfaceNode.addProperty("className", entry.driverClass);
        // Add the driver specific properties
        if (entry.driverClass.equals(KISSviaSerial.class.getName())) {
            interfaceNode.addProperty("serialPort", entry.serialPort);
            interfaceNode.addProperty("baudRate", entry.baudRate);
        } else if (entry.driverClass.equals(KISSviaTCP.class.getName())) {
            interfaceNode.addProperty("ipAddress", entry.ipAddress);
            interfaceNode.addProperty("port", entry.port);
        }
        interfacesNode.addProperty("beaconEvery",0);
        interfacesNode.addProperty("beaconText","");

        config.saveConfig();

        // Tell the
        SingleThreadBus.INSTANCE.post(new ConfigurationChangedEvent());
    }

    @Override
    public String[] getCommandNames() {
        return new String[]{"conf", "configure", "config"};
    }


    private enum State {
        START,
        // KISSviaSerial | KISSviaTNCSerial | KISSviaTCP
        GET_INTERFACE_DRIVER,
        // Serial port settings
        GET_INTERFACE_SERIAL_PORT,
        GET_INTERFACE_BAUD,
        // IP settings
        GET_INTERFACE_IP,
        GET_INTERFACE_PORT,
        // Confirm configuration
        CONFIRM_CONFIGURATION,
        END;
    }

    private class ConfigurationEntry {

        public String driverClass;
        public String serialPort;
        public String baudRate;
        public String ipAddress;
        public int port;

        private ConfigurationEntry() {}

    }

}
