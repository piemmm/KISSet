package org.prowl.kisset.comms.host.parser.commands;

import org.prowl.kisset.comms.host.TNCHost;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.comms.host.parser.Mode;

import java.io.IOException;

public abstract class Command {

    public static final String CR = CommandParser.CR;

    protected TNCHost tncHost;
    protected CommandParser commandParser;

    public Command() {

    }

    public void setClient(TNCHost tncHost, CommandParser commandParser) {
        this.tncHost = tncHost;
        this.commandParser = commandParser;
    }

    /**
     * Execute the command
     *
     * @param data
     * @return true if the command was consumed by this class, false if otherwise.
     * @throws IOException
     */
    public abstract boolean doCommand(String[] data) throws IOException;

    /**
     * This is the command and it's aliases.
     *
     * @return The command and it's aliases.
     */
    public abstract String[] getCommandNames();

    /**
     * Convenience method to write to the client (no detokenisation of strings)
     *
     * @param s
     * @throws IOException
     */
    public void writeToTerminal(String s) throws IOException {
        tncHost.send(s);
    }

    public Mode getMode() {
        return commandParser.getMode();
    }


    public void setMode(Mode mode) {
        commandParser.setMode(mode);
    }

    public void setMode(Mode mode, boolean sendPrompt) throws IOException {
        commandParser.setMode(mode, sendPrompt);
    }


    public void popModeFromStack() {
        commandParser.popModeFromStack();
    }

    public void pushModeToStack(Mode mode) {
        commandParser.pushModeToStack(mode);
    }
}
