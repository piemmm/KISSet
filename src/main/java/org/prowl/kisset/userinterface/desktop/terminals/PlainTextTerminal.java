package org.prowl.kisset.userinterface.desktop.terminals;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
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
 * Java FX component that emulates a terminal that understands alphanumeric printable text only
 * <p>
 * Control characters and ANSI codes are ignored.
 */
public class PlainTextTerminal extends HBox implements Terminal {

    private static final Log LOG = LogFactory.getLog("PlainTextTerminal");

    private static final int maxLines = 1000;
    final Clipboard clipboard = Clipboard.getSystemClipboard();
    final ClipboardContent content = new ClipboardContent();
    private final List<byte[]> buffer = new ArrayList<>();
    /**
     * Stores the height of this line
     */
    List<Integer> lineWidths = new ArrayList<>();


    StringBuilder currentLine = new StringBuilder();
    Canvas canvas = new Canvas();
    ScrollBar vScrollBar = new ScrollBar();
    Font font;
    boolean firstTime = true;
    double charWidth;
    double charHeight;
    double baseline;
    boolean lastByteWasCR = false;
    private volatile Thread redrawThread;
    private BufferPosition startSelect;
    private BufferPosition endSelect;

    public PlainTextTerminal() {
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
                LOG.debug("Copied: " + sb);
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

    private void makeNewLine() {
        synchronized (buffer) {
            String str = ANSI.stripAnsiCodes(currentLine.toString());
            byte[] bytes = str.getBytes();
            buffer.add(bytes);
            lineWidths.add(ANSI.stripAnsiCodes(str).length());

            // If the scrollback buffer is full, then start chopping off the top.
            if (buffer.size() > maxLines) {
                buffer.remove(0);
                lineWidths.remove(0);
            }
        }

        final boolean atMax = vScrollBar.getValue() >= vScrollBar.getMax() - 1;

        Platform.runLater(() -> {

            // Update the scrollbar extents
            vScrollBar.setMin(0);
            vScrollBar.setVisibleAmount(Math.ceil(getHeight() / charHeight));
            int screenful = (int) Math.ceil(getHeight() / charHeight);
            vScrollBar.setMax(Math.max(0, buffer.size() - screenful));

            // Scroll to the bottom and keep it there only if the user is already scrolled at the bottom
            if (atMax || firstTime) {
                firstTime = false;
                vScrollBar.setValue(vScrollBar.getMax());
            }
        });

    }

    public Node getNode() {
        return this;
    }

    private void updateCurrentLine() {
        synchronized (buffer) {
            String str = ANSI.stripAnsiCodes(currentLine.toString());
            byte[] bytes = str.getBytes();
            buffer.set(buffer.size() - 1, bytes);
            lineWidths.set(lineWidths.size() - 1, ANSI.stripAnsiCodes(str).length());
        }
    }

    /**
     * Appends the text to the terminal buffer for later drawing.
     */
    public synchronized final void append(int b) {
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
    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFont(font);
        g.clearRect(0, 0, getWidth(), getHeight());
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFill(Color.WHITE);
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
        synchronized (buffer) {

            int scrollOffset = (int) vScrollBar.getMax() - (int) vScrollBar.getValue();
            for (int i = (buffer.size() - 1) - scrollOffset; i > 0; i--) {

                // Don't bother drawing offscreen stuff.
                if (y < 0) {
                    break;
                }

                byte[] line = buffer.get(i);
                // pre-walk the line so we know how high it is and therefore where to start drawing.
                int lineHeight = calculateNumberOfLines(i);


                x = 0;
                y -= charHeight * lineHeight;
                y = y - extraLine;
                extraLine = 0;


                int skippedAnsi = 0;
                for (int j = 0; j < line.length; j++) {
                    // If the character is an ansi code, then we need to handle it.

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
                        g.fillText(String.valueOf((char) line[j]), x + 1, y);
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
     * Get the character at the specified x,y position on the screen
     * <p>
     * We will need to retrieve this from the relevant array element in the buffer
     *
     * @param x The x position on the screen
     * @param y The y position on the screen
     * @return The positions for the buffer and the character index inside that buffer line
     */
    private BufferPosition getPositionInBuffer(double x, double y) {

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


        return new BufferPosition(i, position);
    }

    public String getName() {
        return "ANSI";
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
