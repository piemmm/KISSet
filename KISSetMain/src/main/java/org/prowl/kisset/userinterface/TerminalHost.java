package org.prowl.kisset.userinterface;

public interface TerminalHost {

    Object getTerminal();

    void setTerminal(Object terminal);

    void setStatus(String statusText, int currentStream);

}
