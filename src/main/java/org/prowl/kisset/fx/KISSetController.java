package org.prowl.kisset.fx;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.fx.FXGraphics2D;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.comms.host.TNCHost;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.gui.terminal.Connection;
import org.prowl.kisset.gui.terminal.Term;
import org.prowl.kisset.gui.terminal.Terminal;
import org.prowl.kisset.util.Tools;

import java.awt.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class KISSetController {

    private static final Log LOG = LogFactory.getLog("KISSetController");
    @FXML
    TextField textEntry;

    @FXML
    ScrollPane theScrollPane;
    @FXML
    StackPane stackPane;
    TerminalCanvas canvas;
    private Terminal term;
    private final PipedInputStream inpis = new PipedInputStream();
    private final PipedOutputStream inpos = new PipedOutputStream();
    private final PipedInputStream outpis = new PipedInputStream();
    private final PipedOutputStream outpos = new PipedOutputStream();

    @FXML
    protected void onQuitAction() {
        // Ask the user if they really want to quit?

        // Quit the application
        KISSet.INSTANCE.quit();
    }

    @FXML
    protected void onPreferencesAction() {
        KISSet.INSTANCE.showPreferences();
    }

    @FXML
    protected void onTextEnteredAction(ActionEvent event) {
        try {
            outpos.write(textEntry.getText().getBytes());
            outpos.write(CommandParser.CR.getBytes());
            outpos.flush();
            textEntry.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setup() {
        term = new Terminal();
        term.setForeGround(Color.WHITE);
        canvas = new TerminalCanvas(term);
        Graphics2D g2d = new FXGraphics2D(canvas.getGraphicsContext2D());
        stackPane.getChildren().add(canvas);

        canvas.setHeight(1280);
        canvas.widthProperty().bind(stackPane.widthProperty());
        Platform.runLater(() -> {
            canvas.heightProperty().bind(stackPane.heightProperty());
        });

        canvas.setOnMouseClicked(event -> {
            textEntry.requestFocus();
        });
        try {
            inpis.connect(inpos);
            outpis.connect(outpos);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Platform.runLater(() -> {

            // Initially the scroll pane to the bottom.
            theScrollPane.setVvalue(Double.MAX_VALUE);


        });

        TNCHost tncHost = new TNCHost(outpis, inpos);
        Tools.runOnThread(() -> {
            term.start(new Connection() {
                @Override
                public InputStream getInputStream() {
                    return inpis;
                }

                @Override
                public OutputStream getOutputStream() {
                    return outpos;
                }

                @Override
                public void requestResize(Term term) {

                }

                @Override
                public void close() {

                }
            });
        });


    }

    static class TerminalCanvas extends Canvas {

        private final FXGraphics2D g2;
        Terminal terminal;

        public TerminalCanvas(Terminal terminal) {
            this.terminal = terminal;
            this.g2 = new FXGraphics2D(getGraphicsContext2D());
            // Redraw canvas when size changes.
            widthProperty().addListener(e -> draw());
            heightProperty().addListener(e -> draw());

        }


        private void draw() {
            Platform.runLater(() -> {
                double width = Math.max(100, getWidth());
                double height = Math.max(100, getHeight());
                terminal.setSize((int) width, (int) height, false);
                this.terminal.paintComponent(g2);
            });
        }

        @Override
        public boolean isResizable() {
            return true;
        }

        @Override
        public double prefWidth(double height) {
            return getWidth();
        }

        @Override
        public double prefHeight(double width) {
            return getHeight();
        }
    }


}