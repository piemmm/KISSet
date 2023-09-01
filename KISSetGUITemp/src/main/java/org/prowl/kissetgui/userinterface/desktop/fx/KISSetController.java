package org.prowl.kissetgui.userinterface.desktop.fx;


import com.google.common.eventbus.Subscribe;
import de.jangassen.MenuToolkit;
import de.jangassen.model.AppearanceMode;
import javafx.application.Platform;
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
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.services.host.TNCHost;
import org.prowl.kisset.services.host.parser.CommandParser;
import org.prowl.kisset.services.host.parser.Mode;
import org.prowl.kisset.userinterface.TerminalHost;
import org.prowl.kisset.util.LoopingCircularBuffer;
import org.prowl.kisset.util.PipedIOStream;
import org.prowl.kisset.util.Tools;
import org.prowl.kissetgui.guiconfig.GUIConf;
import org.prowl.kissetgui.userinterface.desktop.KISSetGUI;
import org.prowl.kissetgui.userinterface.desktop.terminals.*;

import java.awt.*;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;

public class KISSetController implements TerminalHost {

    public static final Class[] TERMINALS = new Class[]{ANSITerminal.class, TeletextTerminal.class, PlainTextTerminal.class, DebugTerminal.class};
    private static final Log LOG = LogFactory.getLog("KISSetController");
    private static final KeyCombination CTRL_C = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
    @FXML
    TextField textEntry;
    @FXML
    MenuBar menuBar;
    @FXML
    MenuItem preferencesMenuItem;
    @FXML
    StackPane stackPane;
    Terminal terminal = new ANSITerminal(); // The default terminal type.
    @FXML
    private Label streamId;
    @FXML
    private Label statusText;
    @FXML
    private ChoiceBox terminalTypeBox;
    private final LoopingCircularBuffer dataBuffer = new LoopingCircularBuffer(10240);
    // TerminalCanvas canvas;
    private PipedIOStream inpis;
    private OutputStream outpos;
    private TNCHost tncHost;


    @FXML
    protected void onQuitAction() {
        // Ask the user if they really want to quit?

        // Quit the application
        KISSet.INSTANCE.quit();
    }

    @FXML
    protected void onShowFBB() {
        KISSetGUI.INSTANCE.showFBB();
    }

    @FXML
    protected void onShowDX() {
        KISSetGUI.INSTANCE.showDX();
    }

    @FXML
    protected void onShowAPRS() {
        KISSetGUI.INSTANCE.showAPRS();
    }

    @FXML
    protected void onPreferencesAction() {
        KISSetGUI.INSTANCE.showPreferences();
    }

    @FXML
    protected void onMonitorAction() {
        KISSetGUI.INSTANCE.showMonitor();
    }

    @FXML
    protected void onDXAction() {
        KISSetGUI.INSTANCE.showDX();
    }

    @FXML
    protected void onFBBAction() {
        KISSetGUI.INSTANCE.showFBB();
    }

    @FXML
    protected void onAPRSAction() {
        KISSetGUI.INSTANCE.showAPRS();
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
        Font font = Font.font(KISSet.INSTANCE.getConfig().getConfig(GUIConf.terminalFont.name(), GUIConf.terminalFont.stringDefault()), fontSize);
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
                            KISSetGUI.INSTANCE.showAbout();
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

        // Create our pipes for streams
        inpis = new PipedIOStream();
        PipedIOStream outpis = new PipedIOStream();
        outpos = outpis.getOutputStream();
        OutputStream inpos = inpis.getOutputStream();

        // Command processor host
        tncHost = new TNCHost(this, outpis, inpos);

        // Start feeding the terminal responses from it
        Tools.runOnThread(() -> {
            try {
                while (true) {
                    int b = inpis.read();
                    dataBuffer.put((byte) b);
                    terminal.append(b);

                }
            } catch (Exception e) {
                LOG.debug(e.getMessage(), e);
            }
        });
    }

    @Override
    public Object getTerminal() {
        return terminal;
    }

    /**
     * Change the terminal type to the new terminal
     *
     * @param terminal
     */
    public void setTerminal(Object terminal) {
        this.terminal = (Terminal) terminal;

        // Pre populate the terminal with our data 'frame' buffer
        byte[] data = dataBuffer.getBytes();
        for (byte b : data) {
            ((Terminal) terminal).append(b & 0xFF);
        }

        Platform.runLater(() -> {

            terminalTypeBox.getSelectionModel().select(terminal.getClass());


            configureTerminal();
        });

    }

    public void setStatus(String status, int currentStream) {
        Platform.runLater(() -> {
            statusText.setText(status);
            streamId.setText(Integer.toString(currentStream));
        });
    }
}