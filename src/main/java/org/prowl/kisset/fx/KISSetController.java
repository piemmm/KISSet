package org.prowl.kisset.fx;


import com.google.common.eventbus.Subscribe;
import de.jangassen.MenuToolkit;
import de.jangassen.model.AppearanceMode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.fx.FXGraphics2D;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.comms.host.TNCHost;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
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

    private static final KeyCombination CTRL_C = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);

    @FXML
    TextField textEntry;
    @FXML
    MenuBar menuBar;
    @FXML
    MenuItem preferencesMenuItem;
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
    private TNCHost tncHost;

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
    protected void onMonitorAction() {
        KISSet.INSTANCE.showMonitor();
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

    @FXML
    protected void onKeyPressed(KeyEvent event) {
        if (CTRL_C.match(event)) {
            tncHost.setMode(Mode.CMD, true);
        }
    }

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
            fontSize = KISSet.INSTANCE.getConfig().getConfig(Conf.terminalFontSize, 14);
        } catch (NumberFormatException e) {

        }
        LOG.debug("Configuring terminal:" + fontSize + "   " + KISSet.INSTANCE.getConfig().getConfig(Conf.terminalFont, "Monospaced"));

        term.setFont(KISSet.INSTANCE.getConfig().getConfig(Conf.terminalFont, "Monospaced"), fontSize);


        canvas = new TerminalCanvas(term);
        stackPane.getChildren().add(canvas);

        canvas.setHeight(1280);
        canvas.widthProperty().bind(stackPane.widthProperty());
        Platform.runLater(() -> {
            canvas.heightProperty().bind(stackPane.heightProperty());
        });

        canvas.setOnMouseClicked(event -> {
            textEntry.requestFocus();
        });


        Platform.runLater(() -> {
            // Initially the scroll pane to the bottom.
            theScrollPane.setVvalue(Double.MAX_VALUE);
        });

    }

    public void setup() {
        SingleThreadBus.INSTANCE.register(this);

        // A little messing around with menubars for macos
        final String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("mac")) {
            menuBar.useSystemMenuBarProperty().set(true);
            MenuToolkit tk = MenuToolkit.toolkit();
            tk.setAppearanceMode(AppearanceMode.AUTO);
            Menu defaultApplicationMenu = tk.createDefaultApplicationMenu("KISSet");
            tk.setApplicationMenu(defaultApplicationMenu);
            Menu fileMenu = preferencesMenuItem.getParentMenu();
            fileMenu.setVisible(false);
            fileMenu.setDisable(true);
            preferencesMenuItem.getParentMenu().getItems().remove(preferencesMenuItem);
            defaultApplicationMenu.getItems().add(1, preferencesMenuItem);
            defaultApplicationMenu.getItems().add(1, new SeparatorMenuItem());
            defaultApplicationMenu.getItems().get(defaultApplicationMenu.getItems().size() - 1).setOnAction(event -> {
                KISSet.INSTANCE.quit();
            });
        } else {
            // traditional menu bar.
        }
        
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


        tncHost = new TNCHost(outpis, inpos);
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