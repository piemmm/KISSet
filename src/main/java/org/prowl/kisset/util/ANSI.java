package org.prowl.kisset.util;

/**
 * This class can take a string token such as %RED% from a supplied String and replace it with the appropriate ANSI colour code.
 */
public class ANSI {

    public static final String NORMAL = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String RED = "\u001B[31m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String YELLOW = "\u001B[33m";
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String ORANGE = "\u001B[38;5;208m";
    public static final String PURPLE = "\u001B[38;5;135m";
    public static final String BLACK = "\u001B[30m";
    public static final String PINK = "\u001B[38;5;205m";
    public static final String INDIGO = "\u001B[38;5;63m";
    public static final String BOLD_RED = "\u001B[1;31m";
    public static final String BOLD_YELLOW = "\u001B[1;33m";
    public static final String BOLD_GREEN = "\u001B[1;32m";
    public static final String BOLD_BLUE = "\u001B[1;34m";
    public static final String BOLD_CYAN = "\u001B[1;36m";
    public static final String BOLD_WHITE = "\u001B[1;37m";

    private static final String[] tokens = new String[]{"%NORMAL%", "%BOLD%", "%UNDERLINE%", "%RED%", "%MAGENTA%", "%YELLOW%", "%GREEN%", "%BLUE%", "%CYAN%", "%WHITE%", "%ORANGE%", "%PURPLE%", "%BLACK%", "%PINK%", "%INDIGO%", "%BOLD_RED%", "%BOLD_YELLOW%", "%BOLD_GREEN%", "%BOLD_BLUE%", "%BOLD_CYAN%, %BOLD_WHITE%"};
    private static final String[] colours = new String[]{ANSI.NORMAL, ANSI.BOLD, ANSI.UNDERLINE, ANSI.RED, ANSI.MAGENTA, ANSI.YELLOW, ANSI.GREEN, ANSI.BLUE, ANSI.CYAN, ANSI.WHITE, ANSI.ORANGE, ANSI.PURPLE, ANSI.BLACK, ANSI.PINK, ANSI.INDIGO, ANSI.BOLD_RED, ANSI.BOLD_YELLOW, ANSI.BOLD_GREEN, ANSI.BOLD_BLUE, ANSI.BOLD_CYAN, ANSI.BOLD_WHITE};

    //private static final String MOVE_TO_START_OF_LINE = "\u001B[1000D";

    /**
     * Convert a string with tokens such as %RED% to ANSI colour codes.
     *
     * @param stringToDetokenize The string to convert
     * @return The converted string
     */
    public static String convertTokensToANSIColours(String stringToDetokenize) {
        for (int i = 0; i < tokens.length; i++) {
            stringToDetokenize = stringToDetokenize.replace(tokens[i], colours[i]);
        }
        return stringToDetokenize;
    }

    public static String stripAnsiCodes(String stringToStrip) {
        return stringToStrip.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    public static String stripKnownColourTokens(String stringToStrip) {
        for (int i = 0; i < tokens.length; i++) {
            stringToStrip = stringToStrip.replace(tokens[i], "");
        }
        return stringToStrip;
    }

}
