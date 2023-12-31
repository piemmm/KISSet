package org.prowl.kisset.services.remote.netrom.user.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.NodeCommand;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.objects.Storage;
import org.prowl.kisset.services.remote.netrom.user.NetROMUserClientHandler;
import org.prowl.kisset.services.remote.netrom.user.parser.commands.Command;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.UnTokenize;
import org.reflections.Reflections;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommandParser {
    // The end character for the BBS prompt
    public static final String PROMPT = ">";
    // Carriage return
    public static final String CR = "\r";
    private static final Log LOG = LogFactory.getLog("CommandParser");
    // Commands that are available
    private static final Set<Class<?>> ALL_COMMANDS = new Reflections("org.prowl.kisset.services.remote.netrom.user.parser.commands").getTypesAnnotatedWith(NodeCommand.class);

    // Command classes that help keep this class cleaner
    private final List<Command> commands = new ArrayList<>();

    // Client we are parsing for
    private final NetROMUserClientHandler client;
    // Stack of modes so we can go back to the previous mode from any command.
    protected List<Mode> modeStack = new ArrayList<>();
    // Default to command mode.
    private Mode mode = Mode.CMD;

    private OutputStream divertStream;


    public CommandParser(NetROMUserClientHandler client) {
        this.client = client;
        // Storage for messages
        Storage storage = KISSet.INSTANCE.getStorage();
        makeCommands();
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
                    command.setClient(client, this);
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

    public void parse(String c) throws IOException {
        try {
            String[] arguments = c.split(" "); // Arguments[0] is the command used.
            if (mode == Mode.CMD) {

                // If the command matches, then we will send the command. It is up to the command to check the mode we are
                // in and act accordingly.
                boolean commandExecuted = false;
                for (Command command : commands) {
                    LOG.debug("Testing command:" + command.getClass().getSimpleName() + " " + arguments[0]);
                    String[] supportedCommands = command.getCommandNames();
                    for (String supportedCommand : supportedCommands) {
                        if (supportedCommand.equalsIgnoreCase(arguments[0])) {
                            commandExecuted = command.doCommand(arguments) | commandExecuted;
                            LOG.debug("Test command:" + command.getClass().getSimpleName() + " " + commandExecuted);
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
                sendPrompt();
            } else if (mode == Mode.CONNECTED_TO_STATION) {

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
                if (!mode.equals(org.prowl.kisset.services.host.parser.Mode.CMD)) {
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
            write("*** Error: " + e.getMessage() + CR);

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
            write(CR + getPrompt());
            client.flush();
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

    public void unknownCommand() throws IOException {
        client.send(CR + ANSI.BOLD_RED + Messages.get("unknownCommand") + ANSI.NORMAL + CR);
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
    public void write(String s) throws IOException {
        client.send(s);
    }

    public void writeRaw(int data) throws IOException {
        client.sendRaw(data);
    }

    /**
     * Convenience method to write a detokenized string
     *
     * @param s
     * @throws IOException
     */
    public void unTokenizeWrite(String s) throws IOException {
        client.send(UnTokenize.str(s));
    }

    public void stop() {
        SingleThreadBus.INSTANCE.unregister(this);
    }

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
}