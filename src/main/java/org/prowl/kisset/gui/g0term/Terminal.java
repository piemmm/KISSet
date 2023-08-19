package org.prowl.kisset.gui.g0term;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Effect;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Java FX component that emulates a terminal that understands some ANSI colour codes.
 * <p>
 * This aims to be memory and cpu efficient by only drawing the visible part of the terminal.
 */
public class Terminal extends HBox {

    private static final Log LOG = LogFactory.getLog("G0Terminal");

    private static final int maxLines = 1000;
    List<byte[]> buffer = new ArrayList<>();

    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();

    /**
     * Stores the height of this line
     */
    List<Integer> lineWidths = new ArrayList<>();

    /**
     * Store color information (in use at the time) so we can iterate backwards in the redraw loop and still use
     * the correct colours at the start of the next line 'down'.
     */
    List<QuickAttribute> attributesInUse = new ArrayList<>();


    StringBuilder currentLine = new StringBuilder();
    private volatile Thread redrawThread;

    private final QuickAttribute decodeQA = new QuickAttribute();
    private final DecodedAnsi decodeDA = new DecodedAnsi(decodeQA, 0);

    Canvas canvas = new Canvas();
    ScrollBar vScrollBar = new ScrollBar();

    private BufferPosition startSelect;
    private BufferPosition endSelect;

    Font font;
    boolean firstTime = true;
    double charWidth;
    double charHeight;
    double baseline;

    public Terminal() {
        super();
        font = Font.font("Monospaced", 12);
        recalculateFontMetrics();
        //getChildren().add(vScrollBar);
        getChildren().add(canvas);
        setPadding(new Insets(0, 0, 0, 0));
        setSpacing(0);

        vScrollBar.setOrientation(Orientation.VERTICAL);
        vScrollBar.setMinWidth(10);
        vScrollBar.setPrefWidth(10);
        vScrollBar.setVisible(true);
        vScrollBar.valueProperty().addListener((observable, oldValue, newValue) -> queueRedraw());
        vScrollBar.setMin(0);
        // vScrollBar.setMax(1000); // This can be lines of text position.
        vScrollBar.setUnitIncrement(1);
        vScrollBar.setBlockIncrement(1);

        canvas.onScrollProperty().set(event -> {
            double value = vScrollBar.getValue() - event.getDeltaY() / 10;
            if (value < 0) {
                value = 0;
            }
            if (value > vScrollBar.getMax()) {
                value = vScrollBar.getMax();
            }

            vScrollBar.setValue(value);
            clearSelection();
        });

        canvas.setOnMousePressed(event -> {
            startSelect = getPositionInBuffer(event.getX(), event.getY());
        });

        canvas.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                endSelect = getPositionInBuffer(event.getX(), event.getY());
                queueRedraw();
            }
        });

        canvas.setOnMouseReleased(event -> {
            endSelect = getPositionInBuffer(event.getX(), event.getY());
            if (endSelect == null || endSelect.equals(startSelect)) {
                clearSelection();
            }

            // If not then we have a selection and we should make sure it's highlighted in the next redraw loop.
            queueRedraw();
        });

        setMinWidth(0);
        setMinHeight(0);
        makeNewLine();

        widthProperty().addListener(evt -> {
            canvas.setWidth(getWidth());
            queueRedraw();
        });

        heightProperty().addListener(evt -> {
            canvas.setHeight(getHeight());
            queueRedraw();
        });

    }

    public boolean hasSelectedArea() {
        return startSelect != null && endSelect != null;
    }

    public void copySelectedTextToClipboard() {
        if (startSelect != null && endSelect != null) {
            if (startSelect != null && endSelect != null) {
//                            BufferPosition select1 = startSelect;
//                            BufferPosition select2 = endSelect;
//                            if (select2.compareTo(select1) > 0) {
//                                BufferPosition tmp = select1;
//                                select1 = select2;
//                                select2 = tmp;
//                            }
                int startLine = Math.min(startSelect.arrayIndex, endSelect.arrayIndex);
                int endLine = Math.max(startSelect.arrayIndex, endSelect.arrayIndex);
                int startCol = Math.min(startSelect.characterIndex, endSelect.characterIndex);
                int endCol = Math.max(startSelect.characterIndex, endSelect.characterIndex);

                if (startLine != endLine) {
                    startCol = 0;
                    endCol = lineWidths.get(endLine);
                }

                StringBuilder sb = new StringBuilder();
                for (int i = startLine; i <= endLine; i++) {
                    byte[] bytes = buffer.get(i);
                    String str = new String(bytes);
                    String stripped = ANSI.stripAnsiCodes(str);
                    if (i == startLine) {
                        stripped = stripped.substring(startCol);
                    }
                    if (i == endLine) {
                        stripped = stripped.substring(0, endCol);
                    }
                    sb.append(stripped);
                    if (i != endLine) {
                        sb.append("\n");
                    }
                }
                LOG.debug("Copied: " + sb.toString());
                content.putString(sb.toString());
                clipboard.setContent(content);
            }
        }
    }

    public void clearSelection() {
        startSelect = null;
        endSelect = null;
        queueRedraw();
    }

    public void setFont(Font font) {
        this.font = font;
        recalculateFontMetrics();
        queueRedraw();
    }

    private void precomputeCurrentLine() {
        byte[] bytes = currentLine.toString().getBytes();
        QuickAttribute previousLine;
        if (attributesInUse.size() > 1) {
            previousLine = attributesInUse.get(attributesInUse.size() - 2);
        } else {
            previousLine = new QuickAttribute();
        }
        QuickAttribute qa = decodeLineAnsi(bytes, previousLine);
        attributesInUse.set(attributesInUse.size() - 1, qa.copy());
        //  LOG.debug("Line:" + Tools.byteArrayToReadableASCIIString(bytes)+"   att:"+qa.color);
    }

    private void makeNewLine() {
        String str = currentLine.toString();
        byte[] bytes = str.getBytes();
        buffer.add(bytes);
        lineWidths.add(ANSI.stripAnsiCodes(str).length());
        attributesInUse.add(new QuickAttribute());

        // If the scrollback buffer is full, then start chopping off the top.
        if (buffer.size() > maxLines) {
            buffer.remove(0);
            lineWidths.remove(0);
            attributesInUse.remove(0);
        }

        Platform.runLater(() -> {

            // Update the scrollbar extents
            vScrollBar.setMin(0);
            vScrollBar.setVisibleAmount(Math.ceil(getHeight() / charHeight));

            // Scroll to the bottom and keep it there only if the user is already scrolled at the bottom
            if (vScrollBar.getValue() == vScrollBar.getMax() || firstTime) {
                firstTime = false;
                vScrollBar.setMax(buffer.size()); // This can be lines of text position.
                vScrollBar.setValue(vScrollBar.getMax());
            } else {
                vScrollBar.setMax(buffer.size()); // This can be lines of text position.
            }
        });

    }

    private void updateCurrentLine() {
        String str = currentLine.toString();
        byte[] bytes = str.getBytes();
        buffer.set(buffer.size() - 1, bytes);
        lineWidths.set(lineWidths.size() - 1, ANSI.stripAnsiCodes(str).length());
    }

    /**
     * Get the ANSI codes in-force at the end of the line passed in
     */
    private QuickAttribute decodeLineAnsi(byte[] line, QuickAttribute qa) {
        for (int j = 0; j < line.length; j++) {
            // If the character is an ansi code, then we need to handle it.
            if (line[j] == 0x1B) {
                DecodedAnsi da = decodeAnsi(line, j, new DecodedAnsi(qa, 0));
                j += da.size;
            } else {
                // Just text
            }
        }
        return qa;
    }

    /**
     * Appends the text to the terminal buffer for later drawing.
     */
    public synchronized final void append(int b) {
        //b = b & 0xFF;
        // Store the byte in the buffer.
        if (b == 10) {
            precomputeCurrentLine();
            currentLine = new StringBuilder(); // don't use .delete as the backing byte[] would never get trimmed.
            makeNewLine();
        } else if (b == 13) {
            // ignore unprintable CR (but we let escape through)
            updateCurrentLine();
        } else {
            currentLine.append((char) b);
            updateCurrentLine();
        }

        queueRedraw();

    }

    /**
     * Avoid pointless redraws by only redrawing when the previous redraw has finished, and at a rate humans
     * can actually notice. This saves CPU cycles and gc churn.
     */
    public synchronized void queueRedraw() {
        if (redrawThread == null) {
            redrawThread = new Thread() {

                public void run() {
                    Tools.delay(15);
                    Platform.runLater(() -> {
                        redrawThread = null;
                        draw();
                    });
                }


            };
            redrawThread.start();
        }
    }

    private int calculateNumberOfLines(int lineNumber) {
        // Now we can calculate the height of the line.
        double charactersWide = Math.ceil((getWidth() - charWidth) / charWidth);
        int numberOfLines = (int) Math.ceil((double) lineWidths.get(lineNumber) / charactersWide);
        if (numberOfLines == 0) {
            numberOfLines = 1;
        }
        return numberOfLines;
    }

    private void recalculateFontMetrics() {
        Text text = new Text("@");
        text.setFont(font);
        charWidth = text.getBoundsInLocal().getWidth();
        charHeight = text.getBoundsInLocal().getHeight();
        baseline = text.getBaselineOffset();
    }

    /**
     * Draws the terminal buffer to the canvas.
     */
    private synchronized void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFont(font);
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setFill(javafx.scene.paint.Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFill(javafx.scene.paint.Color.WHITE);
        g.setStroke(Color.WHITE);

        Color background = null;
        double y = getHeight();
        double x = 0;
        double width = getWidth();
        boolean underline = false;
        boolean bold = false;
        boolean inverse = false;
        int extraLine = 0;
        // We start at the bottom line, and draw upwards and downwards when wrapping lines

        int scrollOffset = (int) vScrollBar.getMax() - (int) vScrollBar.getValue();
        for (int i = (buffer.size() - 1) - scrollOffset; i > 0; i--) {

            // Don't bother drawing offscreen stuff.
            if (y < 0) {
                break;
            }

            byte[] line = buffer.get(i);
            // pre-walk the line so we know how high it is and therefore where to start drawing.
            int lineHeight = calculateNumberOfLines(i);
            QuickAttribute a = attributesInUse.get(Math.max(0, i - 2));
            g.setFill(a.color);
            underline = a.underLine;
            bold = a.bold;
            background = a.bgcolor;

            x = 0;
            y -= charHeight * lineHeight;
            y = y - extraLine;
            extraLine = 0;


            int skippedAnsi = 0;
            for (int j = 0; j < line.length; j++) {
                // If the character is an ansi code, then we need to handle it.
                if (line[j] == 0x1B) {
                    DecodedAnsi da = decodeAnsi(line, j, decodeDA);
                    skippedAnsi += da.size + 1;
                    j += da.size;
                    if (da.qa != null) {
                        g.setFill(da.qa.color);
                        bold = da.qa.bold;
                        underline = da.qa.underLine;
                        background = da.qa.bgcolor;
                    }
                } else {

                    // Now draw the byte array with line wrapping
                    if (x + charWidth >= width) {
                        y += charHeight;
                        extraLine += charHeight;
                        x = 0;
                    }

                    if (startSelect != null && endSelect != null) {
                        BufferPosition select1 = startSelect;
                        BufferPosition select2 = endSelect;
                        if (select2.compareTo(select1) > 0) {
                            BufferPosition tmp = select1;
                            select1 = select2;
                            select2 = tmp;
                        }

                        if (select1.arrayIndex == select2.arrayIndex) {
                            if (select1.arrayIndex == i && select1.characterIndex + skippedAnsi == j) {
                                Effect inverseEffect = new Blend(BlendMode.HARD_LIGHT);
                                g.applyEffect(inverseEffect);
                                inverse = true;
                            }
                            if (select2.arrayIndex == i && select2.characterIndex + skippedAnsi == j) {
                                inverse = false;
                            }
                        } else {
                            if (select1.arrayIndex == i && j == 0) {
                                Effect inverseEffect = new Blend(BlendMode.HARD_LIGHT);
                                g.applyEffect(inverseEffect);
                                inverse = true;
                            }
                            if (select2.arrayIndex == i && j == line.length - 1) {
                                inverse = false;
                            }
                        }
                    }

                    if (inverse) {
                        Paint currentFill = g.getFill();
                        g.setFill(Color.color(0.5, 0.5, 0.5, 0.5));
                        g.fillRect(x, y - charHeight, charWidth + 1, charHeight + (charHeight - baseline));
                        g.setFill(currentFill);
                    } else {
                        g.setEffect(null);
                    }


                    // If background is set, then we need to draw a rectangle first.
                    if (background != null) {
                        Paint currentFill = g.getFill();
                        g.setFill(background);
                        g.fillRect(x, y - charHeight, charWidth + 1, charHeight + (charHeight - baseline));
                        g.setFill(currentFill);
                    }

                    g.fillText(String.valueOf((char) line[j]), x, y);
                    if (bold) {
                        g.fillText(String.valueOf((char) line[j]), x + 1, y + 1);
                    }
                    if (underline) {
                        g.strokeLine(x, y + charHeight - baseline, x + charWidth, y + charHeight - baseline);
                    }
                    x += charWidth;


                }
            }

        }
    }


    /**
     * Change the graphics paint color according to the ANSI codes read from the byte array at the start position
     *
     * @param line
     * @param start
     * @return the new starting point after the ANSI code has been read
     */
    private DecodedAnsi decodeAnsi(byte[] line, int start, DecodedAnsi testDA) {
        QuickAttribute decodeQA = testDA.qa;
        int ansiSize = 0;
        if (start >= line.length) {
            //testDA.qa = null;
            testDA.size = 0;
            return testDA;
        }

        try {
            // Read the ANSI code
            boolean mFound = false;
            StringBuilder ansiCode = new StringBuilder();
            for (int i = start; i < line.length; i++) {
                ansiCode.append((char) line[i]);
                if (line[i] == 'm') {
                    mFound = true;
                    ansiSize += i - start;
                    break;
                }
            }

            if (!mFound) {
                //testDA.qa = null;
                testDA.size = 0;
                return testDA;
            }

            // Now we have the ANSI code, we can decode it.


            String ansiC = ansiCode.toString();
            String[] codes = ansiC.split(";");

            if (!ansiC.contains(";")) {
                codes = new String[]{ansiC};
            }
            for (String code : codes) {
                code = code.replace("[", "").replace("\u001B", "").replace("m", "");
                switch (code) {
                    case "0":
                        // Normal
                        decodeQA.bold = false;
                        decodeQA.underLine = false;
                        decodeQA.color = Color.WHITE;
                        decodeQA.bgcolor = null;
                        break;
                    case "1":
                        // Bold
                        decodeQA.bold = true;
                        break;
                    case "2":
                        // Faint
                        break;
                    case "3":
                        // Italic
                        break;
                    case "4":
                        // Underline
                        decodeQA.underLine = true;
                        break;
                    case "5":
                        // Slow Blink
                        break;
                    case "6":
                        // Rapid Blink
                        break;
                    case "7":
                        // Reverse Video
                        break;
                    case "8":
                        // Conceal
                        break;
                    case "9":
                        // Crossed out
                        break;
                    case "30":
                        decodeQA.color = Color.BLACK;
                        break;
                    case "31":
                        decodeQA.color = Color.RED;
                        break;
                    case "32":
                        decodeQA.color = Color.GREEN.brighter();
                        break;
                    case "33":
                        decodeQA.color = Color.YELLOW;
                        break;
                    case "34":
                        decodeQA.color = Color.BLUE;
                        break;
                    case "35":
                        decodeQA.color = Color.MAGENTA;
                        break;
                    case "36":
                        decodeQA.color = Color.CYAN;
                        break;
                    case "37":
                        decodeQA.color = Color.WHITE;
                        break;
                    case "40":
                        decodeQA.color = Color.BLACK;
                        break;
                    case "41":
                        // Background
                        decodeQA.bgcolor = Color.RED;
                        break;
                    case "42":
                        decodeQA.bgcolor = Color.GREEN;
                        break;
                    case "43":
                        decodeQA.bgcolor = Color.YELLOW;
                        break;
                    case "44":
                        decodeQA.bgcolor = Color.BLUE;
                        break;
                    case "45":
                        decodeQA.bgcolor = Color.MAGENTA;
                        break;
                    case "46":
                        decodeQA.bgcolor = Color.CYAN;
                        break;
                    default:
                        LOG.warn("Unknown ANSI code: " + code);
                        break;
                }
            }

        } catch (Exception e) {
            LOG.error("Error parsing ANSI code:" + Tools.byteArrayToReadableASCIIString(line), e);
        }

        testDA.size = ansiSize;
        testDA.qa = decodeQA;
        return testDA;
    }


    /**
     * Storing state information about the current ansi attributes in use.
     */
    public final class QuickAttribute {
        public Color color;
        public Color bgcolor;
        public boolean underLine;
        public boolean bold;

        public QuickAttribute() {
        }

        public QuickAttribute copy() {
            QuickAttribute qa = new QuickAttribute();
            qa.color = color;
            qa.bgcolor = bgcolor;
            qa.underLine = underLine;
            qa.bold = bold;
            return qa;
        }

    }

    public class DecodedAnsi {
        private int size;
        private QuickAttribute qa;

        public DecodedAnsi(QuickAttribute qa, int size) {
            this.qa = qa;
            this.size = size;
        }

        public DecodedAnsi() {
        }


    }

    /**
     * Get the character at the specified x,y position on the screen
     * <p>
     * We will need to retrieve this from the relevant array element in the buffer
     *
     * @param x The x position on the screen
     * @param y The y position on the screen
     * @return The positions for the buffer and the character index inside that buffer line
     */
    public BufferPosition getPositionInBuffer(double x, double y) {

        //LOG.debug("Clicked Y: " + y);
        double height = getHeight();

        // The line number inside our window
        int lineNo = (int) ((height - y) / charHeight);
        //LOG.debug("Clicked Line: " + lineNo);

        // We need to take into account the scroll bar position
        // Count backwards taking into account the line heights from our precomputed array
        int actualIndex = 0;
        int scrollOffset = (int) vScrollBar.getMax() - (int) vScrollBar.getValue();
        int i;
        for (i = (buffer.size() - 1) - scrollOffset; i > 0; i--) {
            actualIndex += calculateNumberOfLines(i);
            if (actualIndex >= lineNo) {
                break;
            }
        }
        int diff = actualIndex - lineNo;
        //LOG.debug("Diff: " + diff);

        if (i < 0) {
            return null;
        }
        byte[] line = ANSI.stripAnsiCodes(new String(buffer.get(i))).getBytes();
        //LOG.debug("Clicked Line: " + Tools.byteArrayToReadableASCIIString(line));

        // Now get the starting character position of the clicked line.
        int position = (int) ((x / charWidth) + (diff * (int) (getWidth() / charWidth)));
        //LOG.debug("Clicked Position: " + position);
        if (position < 0) {
            position = 0;
        }
        if (position >= line.length) {
            position = line.length - 1;
        }

        // Now get the character at the position
//        byte[] charBytes = new byte[1];
//        charBytes[0] = line[position];


        return new BufferPosition(i, position);
    }


    public class BufferPosition implements Comparable {

        public int arrayIndex;
        public int characterIndex;


        public BufferPosition(int arrayIndex, int characterIndex) {
            this.arrayIndex = arrayIndex;
            this.characterIndex = characterIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BufferPosition that = (BufferPosition) o;
            return arrayIndex == that.arrayIndex && characterIndex == that.characterIndex;
        }

        @Override
        public int hashCode() {
            return Objects.hash(arrayIndex, characterIndex);
        }

        @Override
        public int compareTo(Object o) {
            if (o == null || getClass() != o.getClass()) return 0;
            BufferPosition that = (BufferPosition) o;
            if (arrayIndex == that.arrayIndex) {
                return Integer.compare(that.characterIndex, characterIndex);
            } else {
                return Integer.compare(arrayIndex, that.arrayIndex);
            }
        }
    }
}
