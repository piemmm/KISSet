package org.prowl.kisset.comms.host.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.Messages;
import org.prowl.kisset.annotations.TNCCommand;
import org.prowl.kisset.comms.host.TNCHost;
import org.prowl.kisset.comms.host.parser.commands.Command;
import org.prowl.kisset.util.ANSI;
import org.reflections.Reflections;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommandParser {
    private static final Log LOG = LogFactory.getLog("CommandParser");

    // The end character for the TNC prompt
    public static final String PROMPT = ":";

    // Carriage return
    public static final String CR = "\r\n";

    // Commands that are available
    private static final Set<Class<?>> ALL_COMMANDS = new Reflections("org.prowl.kisset.comms.host.parser.commands").getTypesAnnotatedWith(TNCCommand.class);

    // Command classes that help keep this class cleaner
    private final List<Command> commands = new ArrayList<>();

    // Client we are parsing for
    private final TNCHost tncHost;

    // Default to command mode.
    private Mode mode = Mode.CMD;

    // Stack of modes so we can go back to the previous mode from any command.
    protected List<Mode> modeStack = new ArrayList<>();

    public CommandParser(TNCHost tncHost) {
        this.tncHost = tncHost;
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
                if (instance instanceof Command) {
                    // Setup the command
                    Command command = (Command) instance;
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

    public void parse(String c) throws IOException {

        // Local echo
        write(c);

        if (mode == Mode.CMD || mode == Mode.MESSAGE_LIST_PAGINATION || mode == Mode.MESSAGE_READ_PAGINATION) {
            String[] arguments = c.split(" "); // Arguments[0] is the command used.

            // If the command matches, then we will send the command. It is up to the command to check the mode we are
            // in and act accordingly.
            boolean commandExecuted = false;
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
            }
            if (!commandExecuted && arguments[0].length() > 0) {
                unknownCommand();
            }
            sendPrompt();
        }
    }

    public void sendPrompt() throws IOException {
        try {
            write(CR + getPrompt());
            tncHost.flush();
        } catch (EOFException e) {
            // Connection has gone
        }
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
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
    public void write(String s) throws IOException {
        tncHost.send(s);
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
}