package org.prowl.kissetgui.userinterface.desktop.terminals;

import org.prowl.kisset.util.ANSI;

public class DebugTerminal extends ANSITerminal {

    public DebugTerminal() {
        super();
    }

    /**
     * Appends the text to the terminal buffer for later drawing.
     */
    public synchronized void append(int b) {
        //b = b & 0xFF;
        // LOG.debug("Append:" + Integer.toString(b,16));
        // Store the byte in the buffer.


        if (b == 10) {
            lastByteWasCR = false;
            // Write the hex and update the line
            writeHex(b);
            updateCurrentLine();
        } else if (b == 13) {
            lastByteWasCR = true;
            // Write the hex and update the line
            writeHex(b);
            updateCurrentLine();
            // Newline time
            currentLine = new StringBuilder(); // don't use .delete as the backing byte[] would never get trimmed.
            makeNewLine();
            clearSelection();
        } else {
            // Just write the byte and hexify it if it's not a printable char
            lastByteWasCR = false;
            if (b < 32 || b > 128) {
                writeHex(b);
            } else {
                currentLine.append((char) b);
            }
            updateCurrentLine();

        }

        queueRedraw();

    }
public void writeHex(int b) {
    currentLine.append(ANSI.YELLOW);
    currentLine.append('<');
    currentLine.append(String.format("%02X", b));
    currentLine.append('>');
    currentLine.append(ANSI.NORMAL);
}

    public String toString() {
        return "DebugTerminal";
    }

}
