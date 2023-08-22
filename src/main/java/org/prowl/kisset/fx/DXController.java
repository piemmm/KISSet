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
import org.prowl.kisset.core.Node;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.eventbus.events.DXSpotEvent;
import org.prowl.kisset.eventbus.events.HeardNodeEvent;
import org.prowl.kisset.eventbus.events.InvalidFrameEvent;
import org.prowl.kisset.gui.g0term.Terminal;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.objects.dxcluster.DXSpot;
import org.prowl.kisset.util.ANSI;
import org.prowl.kisset.util.PacketTools;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;

public class DXController {

    private static final Log LOG = LogFactory.getLog("DXController");
    public static final String CR = "\r\n";

    @FXML
    StackPane stackPane;


    private Terminal terminal;
    private PipedInputStream inpis;
    private PipedOutputStream inpos;
    private PipedInputStream outpis;
    private PipedOutputStream outpos;


    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {
        Platform.runLater(() -> {
            terminal.setFont(getFont());
            stackPane.getScene().getWindow().setOpacity(1 - (KISSet.INSTANCE.getConfig().getConfig(Conf.dxTransparency, Conf.dxTransparency.intDefault()) / 100.0));
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
        terminal = new Terminal();

        terminal.setFont(getFont());
        terminal.setFocusTraversable(true);

        terminal.setOnKeyPressed(event -> {
            LOG.debug("Key pressed: " + event.getCode().getName());
            if (terminal.hasSelectedArea() && event.isShortcutDown() && event.getCode().equals(KeyCode.C)) {
                terminal.copySelectedTextToClipboard();
            }
        });

        stackPane.getChildren().add(terminal);


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
            try {
                while (true) {
                    terminal.append(inpis.read());


                }
            } catch (Exception e) {
                LOG.debug(e.getMessage(), e);
            }

        });

        Platform.runLater(() -> {
            try {

                inpos.write((CR + CR + CR + "DX Cluster Monitor - DX spots broadcast over packet will be seen here" + CR).getBytes());
                inpos.flush();
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        });

    }


    // Convenience write class
    private void write(String s) {
        try {
            inpos.write(s.getBytes());
            inpos.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Subscribe
    public void dxHeard(DXSpotEvent event) {
        DXSpot spot = event.getDxSpot();

        write(spot.getLine()+CR);
    }

}