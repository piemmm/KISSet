package org.prowl.kisset.comms.remote.pms.parser.commands;

import org.prowl.kisset.comms.remote.pms.PMSClientHandler;
import org.prowl.kisset.comms.remote.pms.parser.CommandParser;
import org.prowl.kisset.comms.remote.pms.parser.Mode;

import java.io.IOException;

public abstract class Command {

    public static final String CR = CommandParser.CR;

    protected PMSClientHandler client;
    protected CommandParser commandParser;

    public Command() {

    }

    public void setClient(PMSClientHandler client, CommandParser commandParser) {
        this.client = client;
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
    public void write(String s) throws IOException {
        client.send(s);
    }

    public void flush() throws IOException {
        client.flush();
    }

    public Mode getMode() {
        return commandParser.getMode();
    }

    public void setMode(Mode mode) {
        commandParser.setMode(mode);
    }

    public void popModeFromStack() {
        commandParser.popModeFromStack();
    }

    public void pushModeToStack(Mode mode) {
        commandParser.pushModeToStack(mode);
    }
}
