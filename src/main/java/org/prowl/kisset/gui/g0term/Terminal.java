package org.prowl.kisset.gui.g0term;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.Tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Java FX component that emulates a terminal that understands some ANSI colour codes.
 * <p>
 * This aims to be memory and cpu efficient by only drawing the visible part of the terminal.
 */
public class Terminal extends HBox {

    private static final Log LOG = LogFactory.getLog("G0Terminal");

    List<byte[]> buffer = new ArrayList<>();
    List<Integer> lineHeights = new ArrayList<>();
    /**
     * Store color information (in use at the time) so we can iterate backwards in the redraw loop and still use
     * the correct colours at the start of the next line 'down'.
     */
    List<QuickAttribute> attributesInUse = new ArrayList<>();


    StringBuilder currentLine = new StringBuilder();
    private Thread redrawThread;

    private final QuickAttribute decodeQA = new QuickAttribute();
    private final DecodedAnsi decodeDA = new DecodedAnsi(decodeQA, 0);

    Canvas canvas = new Canvas();
    ScrollBar vScrollBar = new ScrollBar();

    Font font;
    boolean firstTime = true;
    double charWidth;
    double charHeight;
    double baseline;

    public Terminal() {
        super();
        font = Font.font("Monospaced", 12);
        recalculateFontMetrics();
        getChildren().add(vScrollBar);
        getChildren().add(canvas);

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

    public void setFont(Font font) {
        this.font = font;
        recalculateFontMetrics();
        queueRedraw();
    }

    public void precomputeCurrentLine() {
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

    public void makeNewLine() {
        byte[] bytes = currentLine.toString().getBytes();
        buffer.add(bytes);
        lineHeights.add(calculateNumberOfLines(bytes));
        attributesInUse.add(new QuickAttribute());

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

    public void updateCurrentLine() {
        byte[] bytes = currentLine.toString().getBytes();
        buffer.set(buffer.size() - 1, bytes);
        lineHeights.set(lineHeights.size() - 1, calculateNumberOfLines(bytes));
    }

    /**
     * Get the ANSI codes in-force at the end of the line passed in
     */
    public QuickAttribute decodeLineAnsi(byte[] line, QuickAttribute qa) {
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
    public synchronized final void append(final int b) {
        // Store the byte in the buffer.
        if (b == 10) {
            precomputeCurrentLine();
            currentLine = new StringBuilder(); // don't use .delete as the backing byte[] would never get trimmed.
            makeNewLine();
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

    public int calculateNumberOfLines(byte[] data) {
        // Strip any ANSI codes so that we have the actual text.
        String text = new String(data);
        text = ANSI.stripAnsiCodes(text);
        // Now we can calculate the height of the line.
        double charactersWide = Math.ceil(getWidth() / charWidth);
        return (int) Math.ceil((double) text.length() / charactersWide);
    }

    public void recalculateFontMetrics() {
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

        double y = getHeight();
        double x = 0;
        double width = getWidth();
        boolean underline = false;
        boolean bold = false;
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
            int lineHeight = calculateNumberOfLines(line);
            QuickAttribute a = attributesInUse.get(Math.max(0, i - 2));
            g.setFill(a.color);
            underline = a.underLine;
            bold = a.bold;

            x = 0;
            y -= charHeight * lineHeight;
            y = y - extraLine;
            extraLine = 0;


            for (int j = 0; j < line.length; j++) {
                // If the character is an ansi code, then we need to handle it.
                if (line[j] == 0x1B) {
                    DecodedAnsi da = decodeAnsi(line, j, decodeDA);
                    j += da.size;
                    if (da.qa != null) {
                        g.setFill(da.qa.color);
                        bold = da.qa.bold;
                        underline = da.qa.underLine;
                    }
                } else {

                    // Now draw the byte array with line wrapping
                    if (x > width) {
                        y += charHeight;
                        extraLine += charHeight;
                        x = 0;
                    } else {
                        g.fillText(String.valueOf((char) line[j]), x, y);
                        if (bold) {
                            g.fillText(String.valueOf((char) line[j]), x + 1, y + 1);
                        }
                        if (underline) {
                            g.strokeLine(x, y + charHeight - baseline, x + charWidth, y + charHeight - baseline);
                        }
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
                        decodeQA.color = Color.RED;
                        break;
                    case "42":
                        decodeQA.color = Color.GREEN;
                        break;
                    case "43":
                        decodeQA.color = Color.YELLOW;
                        break;
                    case "44":
                        decodeQA.color = Color.BLUE;
                        break;
                    case "45":
                        decodeQA.color = Color.MAGENTA;
                        break;
                    case "46":
                        decodeQA.color = Color.CYAN;
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
    public class QuickAttribute {
        public Color color;
        public boolean underLine;
        public boolean bold;

        public QuickAttribute() {
        }

        public QuickAttribute copy() {
            QuickAttribute qa = new QuickAttribute();
            qa.color = color;
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
}
