package org.prowl.kisset.fx;


import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.APRSPacketEvent;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.gui.g0term.Terminal;
import org.prowl.kisset.protocols.aprs.APRSNode;
import org.prowl.kisset.util.Tools;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

public class APRSController {

    private static final Log LOG = LogFactory.getLog("APRSController");
    public static final String CR = "\r\n";

    @FXML
    MapView mapView;

    /**
     * This is the map layer that will display aprs locations.
     */
    private APRSLayer aprsLayer;

    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {

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
        mapView.setZoom(8);
        aprsLayer = new APRSLayer();
        mapView.addLayer(aprsLayer);


    }





    public class APRSLayer extends MapLayer {

        private List<APRSNode> nodes = new ArrayList<>();

        public APRSLayer() {
            super();
            SingleThreadBus.INSTANCE.register(this);
        }

        /**
         * Layout the layer - remember to make the redraw/invalidate call lazy
         */
        @Override
        protected void layoutLayer() {
            for (APRSNode node: nodes) {
                MapPoint location = node.getLocation();
                Node icon = node.getIcon();
                Point2D mapPoint = getMapPoint(location.getLatitude(), location.getLongitude());
                icon.setVisible(true);
                icon.setTranslateX(mapPoint.getX());
                icon.setTranslateY(mapPoint.getY());
            }
        }



        /**
         * Heard an APRS packet, plot it into the map.
         *
         * @param event
         */
        @Subscribe
        public void heardAPRS(APRSPacketEvent event) {



            Platform.runLater(() -> {
                APRSNode node = new APRSNode(event.getAprsPacket());

                if (node.getLocation() == null) {
                    return; // no point if no location
                }
                // shoudl probably dedupe/update heree
                nodes.add(node);

                getChildren().add(node.getIcon());

               markDirty();
            });
        }



    }


}