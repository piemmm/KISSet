package org.prowl.kisset.fx;


import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.fx.FXGraphics2D;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.ax25.AX25Frame;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.core.Node;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.gui.terminal.Connection;
import org.prowl.kisset.gui.terminal.Term;
import org.prowl.kisset.gui.terminal.Terminal;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.awt.*;
import java.io.*;

public class MonitorController {

    private static final Log LOG = LogFactory.getLog("MonitorController");
    public static final String CR = "\r\n";


    @FXML
    ScrollPane theScrollPane;
    @FXML
    StackPane stackPane;
    TerminalCanvas canvas;
    private Terminal term;
    private PipedInputStream inpis;
    private PipedOutputStream inpos;
    private PipedInputStream outpis;
    private PipedOutputStream outpos;


    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {
        Platform.runLater(() -> {
            configureTerminal();
            startTerminal();
        });
    }

    public void configureTerminal() {
        stackPane.getChildren().clear();
        term = new Terminal();
        term.setForeGround(Color.WHITE);
        float fontSize = 14;
        try {
            fontSize = KISSet.INSTANCE.getConfig().getConfig(Conf.terminalFontSize, Conf.terminalFontSize.intDefault());
        } catch (NumberFormatException e) {

        }
        term.setFont(KISSet.INSTANCE.getConfig().getConfig(Conf.terminalFont,  Conf.terminalFont.stringDefault()), fontSize);
        canvas = new TerminalCanvas(term);
        stackPane.getChildren().add(canvas);
        canvas.setHeight(1280);
        canvas.widthProperty().bind(stackPane.widthProperty());
        Platform.runLater(() -> {
            canvas.heightProperty().bind(stackPane.heightProperty());
        });

        Platform.runLater(() -> {
            // Initially the scroll pane to the bottom.
            theScrollPane.setVvalue(Double.MAX_VALUE);
        });
    }

    public void setup() {
        SingleThreadBus.INSTANCE.register(this);
        configureTerminal();
        startTerminal();
    }

    public void startTerminal() {
        inpis = new PipedInputStream();
        inpos = new PipedOutputStream();
        outpis = new PipedInputStream();
        outpos = new PipedOutputStream();

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

        // Start the reader thread for the client.
        Tools.runOnThread(() -> {
            try {
                InputStreamReader reader = new InputStreamReader(outpis);
                BufferedReader bin = new BufferedReader(reader);
                String inLine;
                while ((inLine = bin.readLine()) != null) {
                    //parser.parse(inLine);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });


        //inpis and outpos.
        Platform.runLater(() -> {
            try {
                for (int i = 0; i < 2; i++) {
                    // Ansi code to move down 100 lines
                    inpos.write("\u001b[100B".getBytes());
                }
                inpos.write(("Traffic Monitor" + CR).getBytes());
                inpos.flush();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        });

    }

    @Subscribe
    public void packetReceived(HeardNodeEvent event) {
        Platform.runLater(() -> {
            write(PacketTools.monitorPacketToString(event));
            try { inpos.flush(); } catch(IOException e) { }
        });
    }

    // Convenience write class
    private void write(String s) {
        try {
            inpos.write(s.getBytes());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }


    class TerminalCanvas extends Canvas {

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