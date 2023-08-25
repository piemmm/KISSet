package org.prowl.kisset.gui.terminals;

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
            // ignore unprintable CR (but we let escape through)
            updateCurrentLine();
        } else if (b == 13) {
            lastByteWasCR = true;
            currentLine = new StringBuilder(); // don't use .delete as the backing byte[] would never get trimmed.
            makeNewLine();
            clearSelection();

        } else {
            lastByteWasCR = false;
            if (b < 32 || b > 128) {
                currentLine.append(ANSI.YELLOW);
                currentLine.append('<');
                currentLine.append(Integer.toString(b,16));
                currentLine.append('>');
                currentLine.append(ANSI.NORMAL);
            } else {
                currentLine.append((char) b);
            }
            updateCurrentLine();

        }

        queueRedraw();

    }

}
