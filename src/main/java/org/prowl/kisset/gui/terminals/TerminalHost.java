package org.prowl.kisset.gui.terminals;

public interface TerminalHost {

    void setTerminal(Terminal terminal);

    Terminal getTerminal();

    void setStatus(String status);

}
