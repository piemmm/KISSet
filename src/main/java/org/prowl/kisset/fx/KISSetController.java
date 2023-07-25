package org.prowl.kisset.fx;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.jfree.fx.FXGraphics2D;
import org.prowl.kisset.KISSet;
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

    @FXML
    TextField textEntry;

    @FXML
    StackPane stackPane;

//    @FXML
//    ScrollPane mScrollPane;

    private Terminal term;
    private PipedInputStream inpis = new PipedInputStream();
    private PipedOutputStream inpos = new PipedOutputStream();

    private PipedInputStream outpis = new PipedInputStream();
    private PipedOutputStream outpos = new PipedOutputStream();
    TerminalCanvas canvas;

    @FXML
    protected void onQuitAction() {
        // Ask the user if they really want to quit?

        // Quit the application
        KISSet.INSTANCE.quit();
    }

    @FXML
    protected void onTextEnteredAction(ActionEvent event) {
        try {
            inpos.write(textEntry.getText().getBytes());
            inpos.write('\r');
            inpos.write('\n');
            inpos.flush();
            canvas.draw();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("moo" + textEntry.getText());
    }

    static class TerminalCanvas extends Canvas {

        Terminal terminal;

        private final FXGraphics2D g2;

        public TerminalCanvas(Terminal terminal) {
            this.terminal = terminal;
            this.g2 = new FXGraphics2D(getGraphicsContext2D());
            // Redraw canvas when size changes.
            widthProperty().addListener(e -> draw());
            heightProperty().addListener(e -> draw());

        }


        private void draw() {

            //terminal.setSize(300,300);
            Platform.runLater(() -> {
                double width = Math.max(100, getWidth());
                double height = Math.max(100, getHeight());

                terminal.setSize((int) width, (int) height, false);

                // terminal.setSize((int)width,640);
                // getGraphicsContext2D().clearRect(0, 0, width, height);
                this.terminal.paintComponent(g2);//draw(this.g2, new Rectangle2D.Double(0, 0, width, height));
                //this.terminal.redraw();
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

    public void setup() {
        term = new Terminal();

        term.setForeGround(Color.WHITE);
        canvas = new TerminalCanvas(term);
        Graphics2D g2d = new FXGraphics2D(canvas.getGraphicsContext2D());
        stackPane.getChildren().add(canvas);

        canvas.widthProperty().bind(stackPane.widthProperty());
        canvas.heightProperty().bind(stackPane.heightProperty());

        try {
            inpis.connect(inpos);
            outpis.connect(outpos);
        } catch (Exception e) {
            e.printStackTrace();
        }

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


}