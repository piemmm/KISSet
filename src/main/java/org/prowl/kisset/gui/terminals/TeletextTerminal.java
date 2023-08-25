package org.prowl.kisset.gui.terminals;

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
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.util.Tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Java FX component that emulates a terminal that understands SAA5050 teletext codes.
 * <p>
 * This aims to be memory and cpu efficient by only drawing the visible part of the terminal.
 *
 * This is still a bit of an in-progress mess and will be tidied in due course
 */
public class TeletextTerminal extends HBox implements Terminal {

    int charXPos = 0;
    int charYPos = 0;

    private static final Log LOG = LogFactory.getLog("TeletextTerminal");

    private static final int maxLines = 25;
    private final List<byte[]> buffer = new ArrayList<>();

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
    private final DecodedTeletextChar decodeDA = new DecodedTeletextChar(decodeQA, 0);

    Canvas canvas = new Canvas();

    ScrollBar vScrollBar = new ScrollBar();

    private BufferPosition startSelect;
    private BufferPosition endSelect;

    Font font;
    boolean firstTime = true;
    double charWidth;
    double charHeight;
    double baseline;

    public TeletextTerminal() {


        super();



        //font = Font.font("Monospaced", 12);
       // recalculateFontMetrics();
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

        // Make the initial buffer.
        for (int i = 0; i < 25; i++) {
            makeNewLine();
            currentLine = new StringBuilder();
            currentLine.append(StringUtils.leftPad("", 40, ' '));
            updateCurrentLine();
        }

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
                    String stripped = str;
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
       //this.font = Font.loadFont(KISSet.class.getResource("/fonts/galax/MODE7GX3.TTF").toExternalForm(), 44);
        this.font = Font.loadFont(KISSet.class.getResourceAsStream("/fonts/bedstead/bedstead.otf"), font.getSize()+4);


        LOG.debug("FONT NAME:"+this.font );
      //  this.font = Font.loadFont(getClass().getResourceAsStream("/fonts/bedstead/bedstead.otf"), 55);//font.getSize());


        recalculateFontMetrics();
        queueRedraw();
    }

    public Node getNode() {
        return this;
    }



    private void makeNewLine() {
        synchronized (buffer) {
            String str = currentLine.toString();
            byte[] bytes = str.getBytes();
            buffer.add(bytes);
            lineWidths.add(str.length());
            attributesInUse.add(new QuickAttribute());

            // If the scrollback buffer is full, then start chopping off the top.
            if (buffer.size() > maxLines) {
                buffer.remove(0);
                lineWidths.remove(0);
                attributesInUse.remove(0);
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

    private void updateCurrentLine() {
        synchronized (buffer) {
            String str = currentLine.toString();
            byte[] bytes = str.getBytes();
            buffer.set(buffer.size() - 1, bytes);
            lineWidths.set(lineWidths.size() - 1, str.length());
        }
    }

    /**
     * Get the ANSI codes in-force at the end of the line passed in
     */
    private QuickAttribute decodeLineTeletext(byte[] line, QuickAttribute qa) {
        for (int j = 0; j < line.length; j++) {
            // If the character is an ansi code, then we need to handle it.
            // 127 == black text - part of a differnet spec, for now we will ignore it.
            if ((line[j] & 0xFF) >= 127 || line[j] < 32) {
                DecodedTeletextChar da = decodeTeletextChar(line, j, new DecodedTeletextChar(qa, 0));
                j += da.size;
            } else {
                // Just text
            }
        }
        return qa;
    }

    private boolean inEscape = false;
    private int inPosition =0;
    public void clearScreen() {
        for (int i = 0; i < buffer.size(); i++) {
            byte[] line = buffer.get(i);
            for (int j = 0; j < line.length; j++) {
                line[j] = 32;
            }
        }
    }

    /**
     * Appends the text to the terminal buffer for later drawing.
     */
    public synchronized final void append(int ob) {
        int b = ob & 0xFF;



        if (inEscape) {
            inEscape = false;
            b = 128 + (b % 32);
        }
      //  LOG.debug("Append:" + b + "(" + Integer.toString(b, 16) + ")"+(ob & 0xFF)+": " + (char) b + "   charX:" + charXPos+"   charY:"+charYPos);
        // Not implemented yet
        if (inPosition == 2) {
            // xpos
            inPosition--;
            return;
        } else if (inPosition == 1) {
            // ypos
            inPosition--;
            return;
        }




        // Store the byte in the buffer.
        if (b == 30) {
            // Return cursor to initial position
            charXPos = -1;
            charYPos = 0;
        } else if (b == 8) {
            // Moves cursor one position backwards
            if (charXPos > 0) {
                charXPos--;
            } else if (charXPos == 0) {
                charYPos--;
                charXPos = 38;
            }
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
            charXPos -= 1;
        } else if (b == 11) {
            if (charYPos > 0) {
                charYPos--;
            }
        } else if (b == 13) {
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
        } else {


            if (b != 9) {
                byte[] line = buffer.get(charYPos);
                line[charXPos] = (byte) b;
            }
        }

        if (charXPos == 39) {
            charXPos = 0;
            if (charYPos < 24) {
                charYPos++; // This'll do for the moment.
            } else {
                charYPos = 0;
            }
        } else {
            charXPos++;
        }

        // At the end of the line?
//            currentLine.append((char) b);
//            updateCurrentLine();


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
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFill(Color.WHITE);
        g.setStroke(Color.WHITE);

        Color background = Color.BLACK;
        double y = 0;//getHeight();
        double x = 0;
        double width = getWidth();
        boolean underline = false;
        boolean bold = false;
        boolean inverse = false;
        int extraLine = 0;
        boolean graphics = false;
        boolean lining = false;
        // We start at the bottom line, and draw upwards and downwards when wrapping lines

        int scrollOffset = (int) vScrollBar.getMax() - (int) vScrollBar.getValue();
        //  for (int i = (buffer.size() - 1) - scrollOffset; i >= 0; i--) {
        for (int i = 0; i < buffer.size(); i++) {

            // Don't bother drawing offscreen stuff.
            if (y < 0) {
                break;
            }

            byte[] line = buffer.get(i);

            //QuickAttribute a = attributesInUse.get(Math.max(0, i - 2));
            // g.setFill(a.color);
            underline = false;
            bold = false;
            background = Color.BLACK;
            graphics = false;
            lining = false;
            g.setFill(Color.WHITE);

            x = 0;

            y += charHeight;
            y = y - extraLine;
            extraLine = 0;


            decodeQA.color = Color.WHITE;
            decodeQA.bgcolor = Color.BLACK;
            decodeQA.lining = false;
            decodeQA.graphics = false;
            decodeQA.bold = false;
            int skippedAnsi = 0;
            for (int j = 0; j < line.length; j++) {
                boolean inGraphicsChar = false;
                // If the character is an ansi code, then we need to handle it.
                if ((line[j] & 0xFF) >= 128) {
                    DecodedTeletextChar da = decodeTeletextChar(line, j, decodeDA);
                    skippedAnsi += da.size + 1;
                    j += da.size;
                    if (da.qa != null) {
                        g.setFill(da.qa.color);
                        bold = da.qa.bold;
                        underline = da.qa.underLine;
                        background = da.qa.bgcolor;
                        graphics = da.qa.graphics;
                        lining = da.qa.lining;
                        if (graphics) {
                            inGraphicsChar = true;
                        }
                    }

                }
                //} else {

//                    // Now draw the byte array with line wrapping
//                    if (x + charWidth >= width) {
//                        y += charHeight;
//                        extraLine += charHeight;
//                        x = 0;
//                    }

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
                    g.fillRect(x, y - charHeight+(charHeight-baseline), charWidth , charHeight );
                    g.setFill(currentFill);
                } else {
                    g.setEffect(null);
                }


                // If background is set, then we need to draw a rectangle first.
                if (background != null) {
                    Paint currentFill = g.getFill();
                    g.setFill(background);
                    g.fillRect(x, y - charHeight+(charHeight-baseline), charWidth , charHeight );
                    g.setFill(currentFill);
                }

                // Only draw if it's low byte
                if ((line[j] & 0xFF) < 0x7F || graphics) {
                    int b = (line[j] & 0xFF);
                    if (graphics &&  !(b >=64 && b<=95)) {
                        if (!inGraphicsChar) {
                            // EE00 & EDE0
                            int set = 0xEDE0;
                            if (lining) {
                                set = 0xEE00;
                            }
                            g.fillText(""+(char)(b+set), x, y);
                            //drawSixel(g, b - 32, x, y);
                        }
                    } else {
                        g.fillText(String.valueOf((char) b), x, y);
                    }
                    if (bold) {
                        g.fillText(String.valueOf((char) b), x + 1, y);
                    }
                    //}
                    if (underline) {
                        g.strokeLine(x, y + charHeight - baseline, x + charWidth, y + charHeight - baseline);
                    }
                }
                x += charWidth;


                //}
            }

        }
    }


    /**
     * Change the graphics paint color according to the Teletext codes read from the byte array at the start position
     *
     * @param line
     * @param start
     * @return the new starting point after the ANSI code has been read
     */
    private DecodedTeletextChar decodeTeletextChar(byte[] line, int start, DecodedTeletextChar testDA) {
        QuickAttribute decodeQA = testDA.qa;
        int ansiSize = 0;
        if (start >= line.length) {
            //testDA.qa = null;
            testDA.size = 0;
            testDA.qa = decodeQA;
            return testDA;
        }

        try {
            int code = line[start] & 0xFF;
            //LOG.debug("CODE:" + code);
            switch (code) {
//                case 10:
//                    // Moves cursor one line forward. If it is at the last line of the screen, moves it to the first line unless Data Syntax 3 scroll mode is active.
//                    break;
//                case 11:
//                    // Moves cursor one line backward. If it is at the first line of the screen, moves it to the last line unless Data Syntax 3 scroll mode is active.
//                    break;
//                case 12:
//                    // Resets entire display to spaces with default display attributes and returns the cursor to its initial position.
//                    decodeQA.bold = false;
//                    decodeQA.underLine = false;
//                    decodeQA.color = Color.WHITE;
//                    decodeQA.bgcolor = null;
//                    charXPos = 0;
//                    charYPos = 0;
//                    break;
//                case 30:
//                    // Return cursor to initial position
//                    charXPos = 0;
//                    charYPos = 0;
//                    break;
                case 128:
                    // Black foreground alphabetic
                   // decodeQA.color = Color.BLACK;
                    decodeQA.graphics = false;
                    break;
                case 129:
                    // Alphanumeric red
                    decodeQA.color = Color.RED;
                    decodeQA.graphics = false;
                    break;
                case 130:
                    // Alphanumeric green
                    decodeQA.color = Color.GREEN;
                    decodeQA.graphics = false;

                    break;
                case 131:
                    // Alphanumeric yellow
                    decodeQA.color = Color.YELLOW;
                    decodeQA.graphics = false;

                    break;
                case 132:
                    // Alphanumeric blue
                    decodeQA.color = Color.BLUE;
                    decodeQA.graphics = false;

                    break;
                case 133:
                    // Alphanumeric magenta
                    decodeQA.color = Color.MAGENTA;
                    decodeQA.graphics = false;

                    break;
                case 134:
                    // Alphanumeric cyan
                    decodeQA.color = Color.CYAN;
                    decodeQA.graphics = false;

                    break;
                case 135:
                    // Alphanumeric white
                    decodeQA.color = Color.WHITE;
                    decodeQA.graphics = false;

                    break;
                case 136:
                    // Flash (not supported yet)
                    break;
                case 137:
                    // Steady (not supported yet)
                    break;
                    case 138:
                        // Normal size / end box
                        break;
                case 139:
                    // size control/medium/start box
                    break;
                case 140:
                    // Normal Height
                    break;
                case 141:
                    // Double Height
                    break;
                case 142:
                    // cursor visible
                    break;
                case 143:
                    // invisible cursor
                    break;
                case 144:
                    // Black graphics (not in older spec)
                    decodeQA.color = Color.BLACK;
                    decodeQA.graphics = true;
                case 145:
                    // Graphics Red
                    decodeQA.color = Color.RED;
                    decodeQA.graphics = true;
                    break;
                case 146:
                    // Graphics Green
                    decodeQA.color = Color.GREEN;
                    decodeQA.graphics = true;
                    break;
                case 147:
                    // Graphics Yellow
                    decodeQA.color = Color.YELLOW;
                    decodeQA.graphics = true;
                    break;
                case 148:
                    // Graphics Blue
                    decodeQA.color = Color.BLUE;
                    decodeQA.graphics = true;
                    break;
                case 149:
                    // Graphics Magenta
                    decodeQA.color = Color.MAGENTA;
                    decodeQA.graphics = true;
                    break;
                case 150:
                    // Graphics Cyan
                    decodeQA.color = Color.CYAN;
                    decodeQA.graphics = true;
                    break;
                case 151:
                    // Graphics White
                    decodeQA.color = Color.WHITE;
                    decodeQA.graphics = true;
                    break;
                case 152:
                    // Conceal
                    break;
                case 153:
                    // Stop lining
                    decodeQA.lining = false;
                    // Contiguous Graphics
                    break;
                case 154:
                    decodeQA.lining = true;
                    // start lining
                    // Separated Graphics
                    break;
                case 156:
                    // Black Background
                    decodeQA.bgcolor = Color.BLACK;
                    break;
                case 157:
                    // Set backgroun as current foreground color
                    decodeQA.bgcolor = decodeQA.color;
                    break;
                case 158:
                    // Hold Graphics - image stored as the last received mosaic(graphics) character
                    // basically switching colours without gaps. remembering the next used character going forward
                    break;
                case 159:
                    // Release Graphics
                    decodeQA.graphics = false;
                    break;
                // 160 and above are block graphics characters
                default:
                    //LOG.warn("Unknown Teletext/SAA5050 character code: " + code);
                    break;
            }


        } catch (Exception e) {
            LOG.error("Error parsing Teletext code:" + Tools.byteArrayToReadableASCIIString(line), e);
        }

        testDA.size = 0;
        testDA.qa = decodeQA;
        return testDA;
    }


    /**
     * Storing state information about the current teletext attributes in use.
     */
    public final class QuickAttribute {
        public Color color;
        public Color bgcolor = Color.BLACK;
        public boolean graphics;
        public boolean underLine;
        public boolean bold;
        public boolean lining;

        public QuickAttribute() {
        }

        public QuickAttribute copy() {
            QuickAttribute qa = new QuickAttribute();
            qa.color = color;
            qa.bgcolor = bgcolor;
            qa.graphics = graphics;
            qa.underLine = underLine;
            qa.bold = bold;
            qa.lining = lining;
            return qa;
        }

    }

    public class DecodedTeletextChar {
        private int size;
        private QuickAttribute qa;

        public DecodedTeletextChar(QuickAttribute qa, int size) {
            this.qa = qa;
            this.size = 0;//size;
        }

        public DecodedTeletextChar() {
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

        byte[] line = highToSpace(buffer.get(i));
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

    public byte[] highToSpace(byte[] line) {
        byte[] newLine = new byte[line.length];
        for (int i = 0; i < line.length; i++) {
            if ((line[i] & 0xFF) >= 128) {
                newLine[i] = 32;
            } else {
                newLine[i] = line[i];
            }
        }
        return line;
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

    public String getName() {
        return "Teletext";
    }


    /**
     * Draw a 'sixel'. A sixel is a character composed of 6 blocks (sixels) arranged in a 2x3 grid.
     *
     * Each 'sixel' is the binary representation of the sixelCode passed in from 0 to 63. The sixel is drawn from top left
     * to bottom right, with the top left being the least significant bit.
     *
     * @param sixelCode The numeric sixel code already subtracted by 160 from it's character code so we end up with 0-63
     */
    private void drawSixel(GraphicsContext g, int sixelCode, double x, double y) {
        boolean[] bits = new boolean[6];
        for (int i = 0; i < 6; i++) {
            bits[i] = ((sixelCode >> i) & 0x01) == 1;
        }

        double blockWidth = charWidth / 2d;
        double blockHeight = charHeight / 3d;

        double blockX = x;
        double blockY = y-(charHeight-blockHeight);

        // Draw the 6 blocks
        for (int i = 0; i < 6; i++) {
            if (bits[i]) {
                g.fillRect(blockX, blockY, blockWidth, blockHeight);
            }
            blockX += blockWidth;
            if (i % 2 == 1) {
                blockX = x;
                blockY += blockHeight;
            }
        }
    }

}
