package org.prowl.kisset.fx;


import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.eventbus.events.InvalidFrameEvent;
import org.prowl.kisset.gui.terminals.ANSITerminal;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.protocols.core.Node;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;

public class MonitorController {

    private static final Log LOG = LogFactory.getLog("MonitorController");
    public static final String CR = "\r\n";

    @FXML
    StackPane stackPane;
    @FXML
    ListView<Node> heardList;

    private ObservableList<Node> heardNodes = FXCollections.observableArrayList(new ArrayList<>());
    private ANSITerminal terminal;
    private PipedInputStream inpis;
    private PipedOutputStream inpos;
    private PipedInputStream outpis;
    private PipedOutputStream outpos;


    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {
        Platform.runLater(() -> {
            terminal.setFont(getFont());
            stackPane.getScene().getWindow().setOpacity(1 - (KISSet.INSTANCE.getConfig().getConfig(Conf.monitorTransparency, Conf.monitorTransparency.intDefault()) / 100.0));
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
        terminal = new ANSITerminal();

        terminal.setFont(getFont());
        terminal.setFocusTraversable(true);

        terminal.setOnKeyPressed(event -> {
            LOG.debug("Key pressed: " + event.getCode().getName());
            if (terminal.hasSelectedArea() && event.isShortcutDown() && event.getCode().equals(KeyCode.C)) {
                terminal.copySelectedTextToClipboard();
            }
        });

        stackPane.getChildren().add(terminal);

        Platform.runLater(() -> {

            // Setup the heard list with it's observable array
            heardList.setItems(heardNodes);

            // Also setup the cell renderer for the list
            heardList.setCellFactory(param -> new ListCell<Node>() {
                @Override
                protected void updateItem(Node node, boolean empty) {
                    super.updateItem(node, empty);
                    if (empty || node == null || node.getCallsign() == null) {
                        setText(null);
                    } else {
                        int interfaceNumber = KISSet.INSTANCE.getInterfaceHandler().getInterfaces().indexOf(node.getInterface());
                        setText(interfaceNumber + ": " + node.getCallsign());
                    }
                }
            });
        });

        // Popup context sensitive menu for heard list
        heardList.setOnContextMenuRequested(event -> {
            Node node = heardList.getSelectionModel().getSelectedItem();
            if (node != null) {
                //  KISSet.INSTANCE.getInterfaceHandler().getInterface(node.getInterface()).showContextMenu(heardList, event.getScreenX(), event.getScreenY());
            }
        });
    }

    public void setup() {
        configureTerminal();
        startTerminal();
        SingleThreadBus.INSTANCE.register(this);
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

        Platform.runLater(() -> {
            try {
                inpos.write((CR + CR + CR + "Packet Monitor" + CR).getBytes());
                inpos.flush();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        });

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

    @Subscribe
    public void packetReceived(HeardNodeEvent event) {
        Platform.runLater(() -> {
            write(PacketTools.monitorPacketToString(event));
            try {
                inpos.flush();
            } catch (IOException e) {
            }


            // Don't add invalid packets to the heard list
            if (event.isValidPacket()) {
                Platform.runLater(() -> {
                    heardNodes.remove(event.getNode());
                    heardNodes.add(event.getNode());
                    heardNodes.sort((o1, o2) -> {
                        if (o1.getCallsign() == null) {
                            return 1;
                        }
                        if (o2.getCallsign() == null) {
                            return -1;
                        }
                        return o1.getCallsign().compareTo(o2.getCallsign());
                    });
                });
            }
        });
    }

    @Subscribe
    public void invalidFrameEvent(InvalidFrameEvent event) {
        Interface anInterface = KISSet.INSTANCE.getInterfaceHandler().getInterfaceByUUID(event.connector.getUUID());
        if (anInterface == null) {
            return;
        }
        int interfaceNumber = KISSet.INSTANCE.getInterfaceHandler().getInterfaces().indexOf(anInterface);

        Platform.runLater(() -> {
            write(ANSI.BOLD + interfaceNumber + ANSI.NORMAL + ": " + ANSI.RED + "Invalid frame received:" + ANSI.NORMAL + CR + Tools.byteArrayToReadableASCIIString(event.invalidData) + CR);
            try {
                inpos.flush();
            } catch (IOException e) {
            }
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


}