package org.prowl.kisset.userinterface.stdinout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class intends to convert prestel/teletext codes to ANSI codes.
 * for use in a vt100/xterm compatible terminal
 */
public class StdTeletext extends StdTerminal {

    private static final Log LOG = LogFactory.getLog("StdTeletext");

    private static boolean running = false;


    OutputStream stdOut;

    // Basic screen buffer for visible characters only
    int[][] buffer = new int[25][40];

    public StdTeletext(InputStream stdIn, OutputStream stdOut) {
        super(stdIn, stdOut);
        this.stdOut = stdOut;

        try {
            clearScreen();
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
        }
    }

    // Default std terminal is just a passthrough.
    public void start() {
        running = true;
        // Take std input and pass to TNC host
        Tools.runOnThread(() -> {
            try {
                while (running) {
                    int b = stdIn.read();
                    if (b == -1) break;
                    tncIn.getOutputStream().write(b);
                }
                running = false;
            } catch (Throwable e) {
                LOG.debug(e.getMessage(), e);
            }
        });

        // Take output from TNC host and pass to stdout
        Tools.runOnThread(() -> {
            try {
                setTerminalSize(24, 80);
                while (running) {
                    if (tncOut.available() == 0) {
                        stdOut.flush();
                    }
                    int b = tncOut.read();
                    //stdOut.write("MOOSE".getBytes());//println("STD TELETEXT");
                    if (b == -1) break;
                    b = b & 0xFF;
                    //if (b >= 127 || b < 32) {
                    decodeTeletextChar(b);
                    //} else {
                    //   stdOut.write(b);
                    //}
                }
                running = false;
            } catch (Throwable e) {
                LOG.debug(e.getMessage(), e);
            }
        });

    }

    public void stop() {
        running = false;
    }


    private boolean inEscape = false;
    private int inPosition = 0;
    public String color;
    public String bgcolor = ANSI.BLACK;
    public boolean graphics;
    public boolean underLine;
    public boolean bold;
    public boolean lining;
    public boolean flash;
    // Current cursor position
    private int charXPos = 0;
    private int charYPos = 0;

    public void clearAttributes() throws IOException {
        color = ANSI.WHITE;
        bgcolor = ANSI.BLACK;
        graphics = false;
        flash = false;
        inEscape = false;
        underLine = false;
        bold = false;
        lining = false;
        write(ANSI.NORMAL+ANSI.BG_NORMAL);
    }

    private void decodeTeletextChar(int b) throws IOException {


        if (inEscape) {
            inEscape = false;
            b = 128 + (b % 32);
        }
        //  LOG.debug("Append:" + b + "(" + Integer.toString(b, 16) + ")"+(ob & 0xFF)+": " + (char) b + "   charX:" + charXPos+"   charY:"+charYPos);
        // Not implemented yet - causes us to consume more bytes.
        if (inPosition == 2) {
            // xpos
            inPosition--;
            return;
        } else if (inPosition == 1) {
            // ypos
            inPosition--;
            return;
        }


        if (charXPos >= 40) {

            charXPos = 0;
            if (charYPos < 24) {
                charYPos++; // This'll do for the moment.
            } else {
                charYPos = 0;
            }
            clearAttributes();
      //  } else {
          //
        }


        // Store the byte in the buffer.
        if (b == 30) {
            // Return cursor to initial position
            clearAttributes();
            charXPos = -1;
            charYPos = 0;
        } else if (b == 8) {
            // Moves cursor one position backwards
            if (charXPos > 0) {
                charXPos--;
                charXPos--;
            } else if (charXPos == 0) {
                charYPos--;
                charXPos = 38;

            }
        } else if (b == 9) {
            // One position forward
        } else if (b == 12) {
            clearScreen();
            charXPos = -1;
            charYPos = 0;
        } else if (b == 10) {
            if (charYPos < 24) {
                charYPos++;
            } else {
                charYPos = 0;
            }
            clearAttributes();
            charXPos -= 1;
        } else if (b == 11) {
            if (charYPos > 0) {
                charYPos--;
                charXPos--;
            } else {
                charYPos = 23;
                charXPos--;
            }
        } else if (b == 13) {
            clearAttributes();
//            // ignore unprintable CR (but we let escape through)
//            updateCurrentLine();
            charXPos = -1;
        } else if (b == 27) {
            inEscape = true;
            charXPos--;
        } else if (b == 20) {
            // Cursor off
            charXPos--;
        } else if (b == 31) {
            // Position cursor
            inPosition = 2;
            clearAttributes();
        } else {
            // Overwrite char at X,Y
            if (b != 9 && b < 128) {
                //  byte[] line = buffer.get(charYPos);
                //  line[charXPos] = (byte) b;
                setCursorPosition(charXPos, charYPos);
                if (graphics && (b < 0x40 || b > 0x5F)) {
                    write(getSixelChar(b % 32));
                    buffer[charYPos][charXPos] = b % 32;
                } else {
                    write(b);
                    buffer[charYPos][charXPos] = b;
                }
            }
        }

        charXPos++;

        if (inEscape) {
            return;
        }
        // Now set the cursor position
        setCursorPosition(charXPos, charYPos);



        int code = b;
        if (b >= 127 || b < 32) {
            //LOG.debug("CODE:" + code);
            switch (code) {

                case 128:
                    // Black foreground alphabetic - different spec so we ignore
                    // color = ANSI.BLACK;
                    graphics = false;
                    write(" ");
                    break;
                case 129:
                    // Alphanumeric red
                    color = ANSI.RED;
                    write(ANSI.RED);
                    graphics = false;
                    write(" ");
                    break;
                case 130:
                    // Alphanumeric green
                    color = ANSI.GREEN;
                    write(ANSI.GREEN);
                    graphics = false;
                    write(" ");

                    break;
                case 131:
                    // Alphanumeric yellow
                    color = ANSI.YELLOW;
                    write(ANSI.YELLOW);
                    graphics = false;
                    write(" ");

                    break;
                case 132:
                    // Alphanumeric blue
                    color = ANSI.BLUE;
                    write(ANSI.BLUE);
                    graphics = false;
                    write(" ");

                    break;
                case 133:
                    // Alphanumeric magenta
                    color = ANSI.MAGENTA;
                    write(ANSI.MAGENTA);
                    graphics = false;
                    write(" ");

                    break;
                case 134:
                    // Alphanumeric cyan
                    color = ANSI.CYAN;
                    write(ANSI.CYAN);
                    graphics = false;
                    write(" ");

                    break;
                case 135:
                    // Alphanumeric white
                    color = ANSI.WHITE;
                    write(ANSI.WHITE);
                    graphics = false;
                    write(" ");

                    break;
                case 136:
                    // Flash
                    flash = true;
                    write(ANSI.FLASHING_ON);
                    write(" ");

                    break;
                case 137:
                    // Steady
                    flash = false;
                    write(ANSI.FLASHING_OFF);
                    write(" ");

                    break;
                case 138:
                    // Normal size / end box
                    write(" ");

                    break;
                case 139:
                    // size control/medium/start box
                    write(" ");

                    break;
                case 140:
                    // Normal Height
                   // write(ANSI.DOUBLE_WIDTH);
                    write(" ");

                    break;
                case 141:
                    // Double Height
                    //write(ANSI.DOUBLE_HEIGHT_TOP);
                    write(" ");

                    break;
                case 142:
                    // cursor visible
                    write(" ");

                    break;
                case 143:
                    // invisible cursor
                    write(" ");

                    break;
                case 144:
                    // Black graphics (not in older spec)
                    color = ANSI.BLACK;
                    graphics = true;
                    write(" ");
                    break;
                case 145:
                    // Graphics Red
                    color = ANSI.RED;
                    write(ANSI.RED);
                    graphics = true;
                    write(" ");

                    break;
                case 146:
                    // Graphics Green
                    color = ANSI.GREEN;
                    write(ANSI.GREEN);
                    graphics = true;
                    write(" ");

                    break;
                case 147:
                    // Graphics Yellow
                    color = ANSI.YELLOW;
                    write(ANSI.YELLOW);
                    graphics = true;
                    write(" ");

                    break;
                case 148:
                    // Graphics Blue
                    color = ANSI.BLUE;
                    write(ANSI.BLUE);
                    graphics = true;
                    write(" ");

                    break;
                case 149:
                    // Graphics Magenta
                    color = ANSI.MAGENTA;
                    write(ANSI.MAGENTA);
                    graphics = true;
                    write(" ");

                    break;
                case 150:
                    // Graphics Cyan
                    color = ANSI.CYAN;
                    write(ANSI.CYAN);
                    graphics = true;
                    write(" ");

                    break;
                case 151:
                    // Graphics White
                    color = ANSI.WHITE;
                    write(ANSI.WHITE);
                    graphics = true;
                    write(" ");

                    break;
                case 152:
                    // Conceal
                    write(" ");

                    break;
                case 153:
                    // Stop lining
                    lining = false;
                    // Contiguous Graphics
                    write(" ");

                    break;
                case 154:
                    lining = true;
                    // start lining

                    // Separated Graphics
                    write(" ");

                    break;
                case 156:
                    // Black Background
                    bgcolor = ANSI.BG_BLACK;
                    write(ANSI.BG_NORMAL);
                    write(bgcolor);
                   // write(" ");
                    fillToTheRight();
                    break;
                case 157: // ]
                    // Set background as current foreground color
                    // Change foregrount to background the easy way
                    bgcolor = color.replace("[3", "[4");
                    write(bgcolor);
                    //write(" ");
                    fillToTheRight();
                    break;
                case 158:
                    // Hold Graphics - image stored as the last received mosaic(graphics) character
                    // basically switching colours without gaps. remembering the next used character going forward
                    write(" ");

                    break;
                case 159:
                    // Release Graphics
                    graphics = false;
                    write(" ");
                    break;
                // 160 and above are block graphics characters
                default:
                    //LOG.warn("Unknown Teletext/SAA5050 character code: " + code);
                    break;
            }

        }


    }

    /**
     * Set the terminal size via ANSI
     *
     * @param rows
     * @param cols
     */
    public void setTerminalSize(int rows, int cols) {
        try {
            write("\u001b[8;" + rows + ";" + cols + "t");
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
        }
    }


    /**
     * Fill the screen to the right of the cursor with the current character in the buffer
     * @param line
     */
    public void fillToTheRight() throws IOException{
        for (int x = charXPos; x < 40; x++) {
            write(buffer[charYPos][x]);
        }
    }

    /**
     * Send ansi codes to clear screen
     */
    public void clearScreen() throws IOException {
        write(ANSI.BG_NORMAL);
        clearAttributes();
        cursorHome();
        write("\u001b[2J");
        for (int y = 0; y < 25; y++) {
            for (int x = 0; x < 40; x++) {
                write(" ");
                buffer[y][x] = 32; // Space
            }
            write("\n");
        }
        cursorHome();

    }

    /**
     * Move cursor to home position (0,0)
     */
    public void cursorHome() throws IOException {
        write("\u001b[H");
        charXPos = 0;
        charYPos = 0;
        clearAttributes();
    }

    public void setCursorPosition(int x, int y) throws IOException {
        write("\u001b[" + (y + 1) + ";" + (x+1) + "f");
        // re-apply attributes on this line.
        //readAttributesUntilArriveAtX(x);
    }

    public void write(String s) throws IOException {
        for (byte b : s.getBytes()) {
            stdOut.write(b);
        }
    }

    public void write(int b) throws IOException {
        stdOut.write(b);
    }

    /**
     * Get the closest ascii character for a 2x3 sixel
     */
    public static String getSixelChar(int b) {
        switch (b) {
            case 0:
                return " ";
            case 1:
                return "▘";
            case 2:
                return "▝";
            case 3:
                return "▀";
            case 4:
                return "▖";
            case 5:
                return "▌";
            case 6:
                return "▞";
            case 7:
                return "▛";
            case 8:
                return "▗";
            case 9:
                return "▚";
            case 10:
                return "▐";
            case 11:
                return "▜";
            case 12:
                return "▄";
            case 13:
                return "▙";
            case 14:
                return "▟";
            case 15:
                return "▀"; // 175 // was █
            case 16:
                return "▖";  // 176
            case 17:
                return "▌"; // 177
            case 18:
                return "▞"; // 178
            case 19:
                return "▛"; // 179
            case 20:
                return "▖"; // 180
            case 21:
                return "▌"; // 181
            case 22:
                return "▞"; // 182
            case 23:
                return "▛"; // 183
            case 24:
                return "▞"; // 184
            case 25:
                return "▚"; // 185
            case 26:
                return "▞";  // 186
            case 27:
                return "▜"; // 187
            case 28:
                return "▄"; // 188 // was ▚
            case 29:
                return "▞"; // 189
            case 30:
                return "▞"; // 190
            case 31:
                return "█"; // 191  // was ▛

            default:
                return "?";
        }

    }

}
