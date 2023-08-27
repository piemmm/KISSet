package org.prowl.kisset.gui.terminals;

public interface TerminalHost {

    Terminal getTerminal();

    void setTerminal(Terminal terminal);

    void setStatus(String statusText, int currentStream);

}
