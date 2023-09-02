package org.prowl.kisset.services.host.parser;


import java.util.Objects;

/**
 * This represents the mode of the parser.  The parser can be in different modes depending on the command that was
 * executed, for example message read pagination mode, message list pagination mode, to provide 'continue' prompts to the
 * user
 * <p>
 * Other modes like can be made for example a 'File BBS' mode that uses different sets of plugin provided commands.
 * <p>
 * Plugins implementing the @Commandable annotation check the mode to behaving accordingly.  Commands can overload and use the
 * same command name as other commands, but this should be only done when they use their 'own' mode so as not to
 * intefere with app modes.
 */
public class Mode {

    // Command mode
    public static final Mode CMD = new Mode("CMD");

    // Reading a message mode (for pagination handling)
    public static final Mode MESSAGE_READ_PAGINATION = new Mode("MESSAGE_READ_PAGINATION");

    // Listing messages mode (for pagination handling)
    public static final Mode MESSAGE_LIST_PAGINATION = new Mode("MESSAGE_LIST_PAGINATION");

    // Connected to a station mode
    public static final Mode CONNECTED_TO_STATION = new Mode("CONNECTED_TO_STATION");

    // Sending a private message mode
    public static final Mode SENDING_PRIVATE_MESSAGE = new Mode("SENDING_PRIVATE_MESSAGE");

    // Sending a public/bulletin message mode
    public static final Mode SENDING_PUBLIC_MESSAGE = new Mode("SENDING_PUBLIC_MESSAGE");

    public static final Mode CONFIGURE_INTERFACE = new Mode("CONFIGURE_INTERFACE");


    private final String mode;

    /**
     * Creates a mode object which plugins can also create
     *
     * @param mode
     */
    public Mode(String mode) {
        this.mode = mode;
    }

    public String toString() {
        return mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mode mode1 = (Mode) o;
        return Objects.equals(mode, mode1.mode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode);
    }
}
