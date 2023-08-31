package org.prowl.kisset.fx;

import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.userinterface.desktop.terminals.TeletextTerminal;
import org.prowl.kisset.userinterface.desktop.terminals.Terminal;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A Basic terminal controller window
 * <p>
 * We can use this in later versions to do multiple window stuff or farm connections off to other windows.
 */
public class TerminalController {

    private static final Log LOG = LogFactory.getLog("TerminalController");

    private Terminal terminal;

    private InputStream in;
    private OutputStream out;

    @FXML
    private StackPane stackPane;
    @FXML
    private TextField textEntry;

    @FXML
    protected void onTextEnteredAction() {

        try {
            out.write((textEntry.getText() + "\r\n").getBytes());
            out.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        textEntry.setText("");
    }

    @FXML
    protected void onKeyPressed() {

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


    public void setup() {
        stackPane.getChildren().clear();
        terminal = new TeletextTerminal();
        terminal.setFont(getFont());

//        terminal.setFocusTraversable(true);
//        terminal.setOnKeyPressed(event -> {
//            LOG.debug("Key pressed: " + event.getCode().getName());
//            if (terminal.hasSelectedArea() && event.isShortcutDown() && event.getCode().equals(KeyCode.C)) {
//                terminal.copySelectedTextToClipboard();
//            }
//        });
        stackPane.getChildren().add(terminal.getNode());

        terminal.getNode().setOnMouseClicked(event -> {
            stackPane.requestFocus();
        });


    }

    /**
     * Start processing the i/o on the terminal
     */
    public void start() {
        Tools.runOnThread(() -> {
            try {
//                terminal.append(27);
//                terminal.append(0x93-64);
//                for (int i = 0; i < 64; i++) {
//                    terminal.append(32);
//                    terminal.append(32);
//                    terminal.append(i+32);
//                    terminal.append(32);
//                    terminal.append(32);
//                    if (i % 8 == 7) {
//                        terminal.append(13);
//                        terminal.append(10);
//                        terminal.append(13);
//                        terminal.append(10);
//                        terminal.append(27);
//                        terminal.append(84-64);
//                    }
//                }


                while (true) {
                    terminal.append(in.read());
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    /**
     * Streams to use for the terminal
     *
     * @param in
     * @param out
     */
    public void setConnection(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }


    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {
        Platform.runLater(() -> {
            terminal.setFont(getFont());
            stackPane.getScene().getWindow().setOpacity(1 - (KISSet.INSTANCE.getConfig().getConfig(Conf.monitorTransparency, Conf.monitorTransparency.intDefault()) / 100.0));
        });
    }

}
