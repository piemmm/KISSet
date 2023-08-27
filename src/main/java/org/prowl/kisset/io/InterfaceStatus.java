package org.prowl.kisset.io;

public class InterfaceStatus {

    private final State state;
    private final String message;

    public InterfaceStatus(State state, String message) {
        this.state = state;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public State getState() {
        return state;
    }

    public enum State {
        OK,
        WARN,
        ERROR
    }
}
