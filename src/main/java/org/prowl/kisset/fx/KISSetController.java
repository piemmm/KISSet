package org.prowl.kisset.fx;


import com.google.common.eventbus.Subscribe;
import de.jangassen.MenuToolkit;
import de.jangassen.model.AppearanceMode;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.util.StringConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.Messages;
import org.prowl.kisset.comms.host.TNCHost;
import org.prowl.kisset.comms.host.parser.CommandParser;
import org.prowl.kisset.comms.host.parser.Mode;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.gui.terminals.ANSITerminal;
import org.prowl.kisset.gui.terminals.TeletextTerminal;
import org.prowl.kisset.gui.terminals.Terminal;
import org.prowl.kisset.gui.terminals.TerminalHost;
import org.prowl.kisset.util.Tools;

import java.awt.*;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;

public class KISSetController implements TerminalHost {

    private static final Log LOG = LogFactory.getLog("KISSetController");

    private static final KeyCombination CTRL_C = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);

    public static final Class[] TERMINALS = new Class[]{ANSITerminal.class, TeletextTerminal.class};

    @FXML
    TextField textEntry;
    @FXML
    MenuBar menuBar;
    @FXML
    MenuItem preferencesMenuItem;
    @FXML
    StackPane stackPane;
    @FXML
    private Label streamId;
    @FXML
    private Label statusText;
    @FXML
    private ChoiceBox terminalTypeBox;

    Terminal terminal = new ANSITerminal(); // The default terminal type.
    // TerminalCanvas canvas;
    private PipedInputStream inpis;
    private PipedOutputStream outpos;
    private TNCHost tncHost;

    @FXML
    protected void onQuitAction() {
        // Ask the user if they really want to quit?

        // Quit the application
        KISSet.INSTANCE.quit();
    }

    @FXML
    protected void onShowFBB() {
        KISSet.INSTANCE.showFBB();
    }

    @FXML
    protected void onShowDX() {
        KISSet.INSTANCE.showDX();
    }

    @FXML
    protected void onShowAPRS() {
        KISSet.INSTANCE.showAPRS();
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
    protected void onDXAction() {
        KISSet.INSTANCE.showDX();
    }

    @FXML
    protected void onFBBAction() {
        KISSet.INSTANCE.showFBB();
    }

    @FXML
    protected void onAPRSAction() {
        KISSet.INSTANCE.showAPRS();
    }

    @FXML
    protected void onTextEnteredAction(ActionEvent event) {
        try {
            outpos.write(textEntry.getText().getBytes());
            outpos.write(CommandParser.CR.getBytes());
            outpos.flush();
            textEntry.clear();
            terminal.clearSelection();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    protected void onKeyPressed(KeyEvent event) {
        // Handle copy/paste
        if (event.isShortcutDown() && event.getCode() == KeyCode.C && terminal.hasSelectedArea()) {
            terminal.copySelectedTextToClipboard();
        }

        if (!terminal.hasSelectedArea() && CTRL_C.match(event)) {
            tncHost.setMode(Mode.CMD, true);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {
        Platform.runLater(() -> {
            terminal.setFont(getFont());
            stackPane.getScene().getWindow().setOpacity(1 - (KISSet.INSTANCE.getConfig().getConfig(Conf.terminalTransparency, Conf.terminalTransparency.intDefault()) / 100.0));
        });
    }

    public Font getFont() {
        float fontSize = 14;
        try {
            fontSize = KISSet.INSTANCE.getConfig().getConfig(Conf.terminalFontSize, Conf.terminalFontSize.intDefault());
        } catch (NumberFormatException e) {

        }
        Font font = Font.font(KISSet.INSTANCE.getConfig().getConfig(Conf.terminalFont, Conf.terminalFont.stringDefault()), fontSize);
        return font;
    }

    public void configureTerminal() {
        stackPane.getChildren().clear();
        terminal.setFont(getFont());
        stackPane.getChildren().add(terminal.getNode());
        terminal.getNode().setOnMouseClicked(event -> {
            textEntry.requestFocus();
        });
    }


    public void setup() {
        SingleThreadBus.INSTANCE.register(this);

        // A little messing around with menubars for macos
        final String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().startsWith("mac")) {
            MenuToolkit tk = MenuToolkit.toolkit();

            menuBar.useSystemMenuBarProperty().set(true);
            menuBar.getMenus().add(0, tk.createDefaultApplicationMenu("KISSet"));
            tk.setGlobalMenuBar(menuBar);

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

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().setAboutHandler(new AboutHandler() {
                    @Override
                    public void handleAbout(AboutEvent e) {
                        Platform.runLater(() -> {
                            KISSet.INSTANCE.showAbout();
                        });

                    }
                });
            }
        } else {
            // traditional menu bar.
        }

        // Setup the terminal type box
        Arrays.stream(TERMINALS).forEach(terminalTypeBox.getItems()::add);
        terminalTypeBox.setConverter(new StringConverter() {
            @Override
            public String toString(Object object) {
                if (object == null) {
                    return "";
                }
                return (Messages.get(((Class) object).getSimpleName()));
            }

            @Override
            public Object fromString(String string) {
                // Unsupported.
                return null;
            }
        });
        // We always start in ANSI mode.
        terminalTypeBox.getSelectionModel().select(ANSITerminal.class);
        terminalTypeBox.getSelectionModel().selectedIndexProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    try {
                        Constructor constructor = ((Class) terminalTypeBox.getItems().get(newValue.intValue())).getConstructor();
                        setTerminal((Terminal) constructor.newInstance());

                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
        });

        configureTerminal();
        startTerminal();
    }

    public void startTerminal() {
        inpis = new PipedInputStream();
        PipedOutputStream inpos = new PipedOutputStream();
        PipedInputStream outpis = new PipedInputStream();
        outpos = new PipedOutputStream();

        try {
            inpis.connect(inpos);
            outpis.connect(outpos);
        } catch (Exception e) {
            e.printStackTrace();
        }

        tncHost = new TNCHost(this, outpis, inpos);

        Tools.runOnThread(() -> {
            try {
                while (true) {
                    terminal.append(inpis.read());
                }
            } catch (Exception e) {
                LOG.debug(e.getMessage(), e);
            }
        });
    }

    /**
     * Change the terminal type to the new terminal
     *
     * @param terminal
     */
    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;

        Platform.runLater(() -> {
            configureTerminal();
        });

    }

    @Override
    public Terminal getTerminal() {
        return terminal;
    }
}