package org.prowl.kisset.services.host.parser;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangeCompleteEvent;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.io.Stream;
import org.prowl.kisset.services.host.TNCHost;
import org.prowl.kisset.services.host.parser.commands.ChangeInterface;
import org.prowl.kisset.services.host.parser.commands.Command;
import org.prowl.kisset.util.ANSI;
import org.reflections.Reflections;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommandParser {
    // The end character for the TNC prompt
    public static final String PROMPT = ":";
    // Carriage return
    public static final String CR = "\r\n";
    private static final Log LOG = LogFactory.getLog("CommandParser");
    // Commands that are available
    private static final Set<Class<?>> ALL_COMMANDS = new Reflections("org.prowl.kisset.services.host.parser.commands").getTypesAnnotatedWith(TNCCommand.class);
    static {
        ALL_COMMANDS.addAll(new Reflections("org.prowl.kisset.services.host.parser.guicommands").getTypesAnnotatedWith(TNCCommand.class));
    }

    // Command classes that help keep this class cleaner
    private final List<Command> commands = new ArrayList<>();

    // Client we are parsing for
    private final TNCHost tncHost;
    // Stack of modes so we can go back to the previous mode from any command.
    protected List<Mode> modeStack = new ArrayList<>();
    // Default to command mode.
    private Mode mode = Mode.CMD;

    private Interface currentInterface;

    private OutputStream divertStream;

    // Local echo defaults to on
    private boolean localEchoEnabled = true;

    public CommandParser(TNCHost tncHost) {
        this.tncHost = tncHost;
        makeCommands();
        finishInit();
        SingleThreadBus.INSTANCE.register(this);
    }

    public void setLocalEcho(boolean enable) {
        this.localEchoEnabled = enable;
    }

    public void updateStatus() {
        tncHost.updateStatus();
    }

    /**
     * Instantiate all the available commands in the CommandItem enum - this lets others add commands as external jars
     * without having to modify this class by using the @Commandable annotation in their command class.
     *
     * @throws IOException
     */
    public void makeCommands() {
        for (Class<?> cl : ALL_COMMANDS) {
            try {
                Object instance = cl.getDeclaredConstructor(new Class[0]).newInstance();
                if (instance instanceof Command command) {
                    // Setup the command
                    command.setClient(tncHost, this);
                    commands.add(command);
                } else {
                    // This isn't a commandable class!
                    LOG.fatal("Class is not a command: " + cl);
                    System.exit(1);
                }
            } catch (Exception e) {
                LOG.error("Unable to instantiate command: " + cl, e);
            }
        }
    }

    public void finishInit() {
        List<Interface> interfaces = KISSet.INSTANCE.getInterfaceHandler().getInterfaces();
        // Default to getting the first interface
        if (interfaces.size() > 0) {
            currentInterface = interfaces.get(0);
        } else {
            currentInterface = null;
        }


    }

    @Subscribe
    public void refreshConfiguraton(ConfigurationChangeCompleteEvent event) {
        try {
            if (currentInterface == null || !currentInterface.isRunning()) {
                List<Interface> interfaces = KISSet.INSTANCE.getInterfaceHandler().getInterfaces();
                if (interfaces.size() > 0) {
                    if (event.interfacesWereChanged()) {
                        currentInterface = interfaces.get(0);
                        writeToTerminal(CR + ANSI.YELLOW + "*** Current KISS interface has been changed due to an interface reconfiguration" + ANSI.NORMAL + CR);
                    } else {
                        // Existing interfaces, so should always be present so just select the new instance again.
                        if (currentInterface != null) {
                            currentInterface = KISSet.INSTANCE.getInterfaceHandler().getInterfaceByUUID(currentInterface.getUUID());
                        } else {
                            currentInterface = interfaces.get(0);
                        }
                    }
                } else {
                    writeToTerminal(CR+ANSI.RED+"*** There is no available KISS interface due to a configuration change"+ANSI.NORMAL+CR);
                    currentInterface = null;
                }
            }
        } catch(Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void parse(String c) throws IOException {

        try {
            // Local echo
            if (localEchoEnabled) {
                writeToTerminal(c + CR);
            }
            String[] arguments = c.split(" "); // Arguments[0] is the command used.

            if (mode == Mode.CMD || mode == Mode.MESSAGE_LIST_PAGINATION || mode == Mode.MESSAGE_READ_PAGINATION) {

                // If the command matches, then we will send the command. It is up to the command to check the mode we are
                // in and act accordingly.
                boolean commandExecuted = false;
                try {

                    // Go through each command and they should only execute if they match the command name
                    // and the mode requirements for that command (usually Mode.CMD which is decided by the actual command).
                    for (Command command : commands) {
                        String[] supportedCommands = command.getCommandNames();
                        for (String supportedCommand : supportedCommands) {
                            if (supportedCommand.equalsIgnoreCase(arguments[0])) {
                                commandExecuted = command.doCommand(arguments) | commandExecuted;
                                // Stop when we executed a command.
                                if (commandExecuted) {
                                    break;
                                }
                            }
                        }
                        if (commandExecuted) {
                            break;
                        }
                    }

                    if (!commandExecuted && arguments[0].length() > 0) {
                        unknownCommand();
                    }
                } catch (IOException e) {
                    // Thrown if the command generated an error (like a connector not being setup due to a missing
                    // serial port device.
                    LOG.error(e.getMessage(), e);
                    writeToTerminal("*** Error: " + e.getMessage() + CR);
                }
                if (mode == Mode.CMD) {
                    sendPrompt();
                }
            } else if (mode == Mode.CONNECTED_TO_STATION || mode == Mode.CONNECTED_TO_INTERNET) {

                // Send i/o to/from station
                if (divertStream != null) {
                    try {
                        divertStream.write(c.getBytes());
                        divertStream.write('\r');
                        divertStream.flush();
                    } catch (IOException e) {
                        LOG.error("Unable to write to divert stream", e);
                    }
                }

            } else {
                // Try in case of a paticular mode setting requires 'no command' when outside of command mode
                if (!mode.equals(Mode.CMD)) {
                    for (Command command : commands) {
                        if (command.doCommand(arguments)) {
                            LOG.debug("Command executed: " + command.getClass().getSimpleName());
                            break;
                        }
                    }

                }
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            writeToTerminal("*** Error: " + e.getMessage() + CR);

        }
    }

    public void closeDivertStream() {
        if (divertStream != null) {
            try {
                divertStream.close();
            } catch (IOException e) {
                LOG.error("Unable to close divert stream", e);
            }
        }
    }

    public void setDivertStream(OutputStream divertStream) {
        this.divertStream = divertStream;
    }

    public void sendPrompt() throws IOException {
        try {
            writeToTerminal(CR + getPrompt());
            tncHost.flush();
        } catch (EOFException e) {
            // Connection has gone
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setMode(Mode mode, boolean sendPrompt) {
        this.mode = mode;
        if (sendPrompt) {
            try {
                sendPrompt();
            } catch (IOException e) {
                // Ignore this one.
            }
        }
    }

    public void setModeIfCurrentStream(Mode mode, Stream callerStream) {
        if (currentInterface != null && getCurrentInterface().getCurrentStream() == callerStream) {
            setMode(mode);
        }
    }


    public void setModeIfCurrentStream(Mode mode, Stream callerStream, boolean sendPrompt) {
        if (currentInterface != null && getCurrentInterface().getCurrentStream() == callerStream) {
            setMode(mode, sendPrompt);
        }
    }

    public void unknownCommand() throws IOException {
        tncHost.send(CR + CR + ANSI.BOLD_RED + Messages.get("unknownCommand") + ANSI.NORMAL + CR);
    }

    public String getPrompt() {
        String name = Messages.get(mode.toString().toLowerCase());
        return ANSI.BOLD_YELLOW + name + ANSI.BOLD_WHITE + PROMPT + ANSI.NORMAL + " ";
    }

    /**
     * Convenience method to write and not detokenize a string
     *
     * @param s
     * @throws IOException
     */
    public void writeToTerminal(String s) throws IOException {
        tncHost.send(s);
    }

    public void writeRawToTerminal(int data) throws IOException {
        tncHost.writeDirect(data);
    }

    /**
     * Convenience method to write a detokenized string
     *
     * @param s
     * @throws IOException
     */
//    public void unTokenizeWrite(String s) throws IOException {
//        tncHost.send(UnTokenize.str(s));
//    }

//    public void stop() {
//        ServerBus.INSTANCE.unregister(this);
//    }
    public void pushModeToStack(Mode mode) {
        modeStack.add(mode);
    }

    public Mode popModeFromStack() {
        if (modeStack.size() > 0) {
            mode = modeStack.get(modeStack.size() - 1);
            modeStack.remove(modeStack.size() - 1);
        } else {
            mode = Mode.CMD;
        }
        return mode;
    }

    public Interface getCurrentInterface() {
        return currentInterface;
    }

    public void setCurrentInterface(Interface currentInterface) {
        this.currentInterface = currentInterface;
    }
}