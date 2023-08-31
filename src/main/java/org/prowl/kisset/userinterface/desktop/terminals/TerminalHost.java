package org.prowl.kisset.userinterface.desktop.terminals;

public interface TerminalHost {

    Terminal getTerminal();

    void setTerminal(Terminal terminal);

    void setStatus(String statusText, int currentStream);

}
