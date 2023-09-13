package org.prowl.kisset.services.remote.netrom.user.parser;


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
    public static final Mode CMD = new Mode("NODE_CMD");
    public static final Mode CONNECTED_TO_STATION = new Mode("NODE_CONNECTED_TO_STATION");


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
