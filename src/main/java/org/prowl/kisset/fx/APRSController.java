package org.prowl.kisset.fx;


import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.APRSPacketEvent;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.protocols.aprs.APRSNode;
import org.prowl.kisset.protocols.aprs.TrackShape;

import java.util.*;

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


    /**
     * Setup the map
     */
    public void setup() {
        mapView.setZoom(8);
        aprsLayer = new APRSLayer();
        mapView.addLayer(aprsLayer);
    }

    public class APRSLayer extends MapLayer {

        private Map<String,APRSNode> nodeMap = Collections.synchronizedMap(new HashMap<>());

        public APRSLayer() {
            super();
            SingleThreadBus.INSTANCE.register(this);
        }

        /**
         * Layout the layer - remember to make the redraw/invalidate call lazy
         */
        @Override
        protected void layoutLayer() {

            Collection<APRSNode> nodes = nodeMap.values();
            for (APRSNode node: nodes) {
               updateNode(node);
               updateTrack(node);
            }
        }

        public Point2D getMapPointExt(double lat, double lon) {
            return getMapPoint(lat, lon);
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

                if (node.getLocation().getLatitude() == 0 && node.getLocation().getLongitude() == 0) {
                    return; // we're treating 0,0 as an invalid GPS location that's just 'junk' data.
                }

                APRSNode existingNode = nodeMap.get(node.getSourceCallsign());
                if (existingNode == null) {
                    // Add new node to map
                    nodeMap.put(node.getSourceCallsign(), node);
                    //getChildren().add(node.getIcon());
                    addNode(node, node.getLocation());
                } else {
                    // Update existing node, possibly adding a track line.
                    boolean firstAddPolyline = false;
                    Polyline polyLine = existingNode.getTrack();
                    if (polyLine == null) {
                        // first polyline, so add
                        firstAddPolyline = true;
                    }
                    existingNode.updateLocation(event.getAprsPacket().getRecevedTimestamp().getTime(), node.getLocation(), aprsLayer);
                    if (firstAddPolyline) {
                      //  addNode(existingNode.getTrack(), existingNode.getLocation());
                        getChildren().add(existingNode.getTrack());
                    }
                    updateNode(existingNode);


                }

              // markDirty();
            });
        }

        public void updateNode(APRSNode node) {
            MapPoint location = node.getLocation();

            // Get the icon
            Node icon = node.getIcon();
            Point2D mapPoint = getMapPoint(location.getLatitude(), location.getLongitude());
            icon.setVisible(true);
            icon.setTranslateX(mapPoint.getX() - (16));
            icon.setTranslateY(mapPoint.getY() - 16);

        }

        public void updateTrack(APRSNode node) {
            // Same for any track poly
            TrackShape trackLine = node.getTrack();
            if (trackLine != null) {
                trackLine.update();
            }
        }


        public void addNode(APRSNode aprsNode, MapPoint location) {
            // Get the icon
            Node node = aprsNode.getIcon();
            Point2D mapPoint = getMapPoint(location.getLatitude(), location.getLongitude());
            node.setVisible(true);
            node.setTranslateX(mapPoint.getX()-(16));
            node.setTranslateY(mapPoint.getY()-16);

            Tooltip t = new Tooltip(aprsNode.getSourceCallsign());
            Tooltip.install(node, t);

            getChildren().add(node);
        }

    }


}