package org.prowl.kisset.fx;


import com.google.common.eventbus.Subscribe;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.maps.MapLayer;
import org.prowl.maps.MapPoint;
import org.prowl.maps.MapView;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.APRSPacketEvent;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.protocols.aprs.APRSNode;
import org.prowl.kisset.protocols.aprs.TrackShape;

import java.util.*;

public class APRSController {

    public static final String CR = "\r\n";
    private static final Log LOG = LogFactory.getLog("APRSController");
    @FXML
    MapView mapView;
    @FXML
    Label searchResultsLabel;
    @FXML
    TextField searchBox;


    /**
     * This is the map layer that will display aprs locations.
     */
    private APRSLayer aprsLayer;

    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {

    }

    @FXML
    private void onSearch() {
        String searchText = searchBox.getText();

        List<APRSNode> results = aprsLayer.search(searchText);
        if (results.size() == 0) {
            searchResultsLabel.setText("No results");
        } else {
            searchResultsLabel.setText(results.size() + " results");
        }

        // Now tell the map to redraw around all of the nodes as a bounding box for all the results combined, so that
        // they are all visible
        if (results.size() > 0) {
            mapView.setCenter(results.get(0).getLocation().getLatitude(), results.get(0).getLocation().getLongitude());
        }

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

        private final Map<String, APRSNode> nodeMap = Collections.synchronizedMap(new HashMap<>());

        public APRSLayer() {
            super();
            SingleThreadBus.INSTANCE.register(this);
        }

        public void clear() {
            nodeMap.clear();
            getChildren().clear();
        }

        /**
         * Search for a callsign and return a list of matching nodes. Wildcards of * are allowed.
         */
        public List<APRSNode> search(String searchText) {
            List<APRSNode> results = new ArrayList<>();
            for (APRSNode node : nodeMap.values()) {
                if (node.getSourceCallsign().matches(searchText)) {
                    results.add(node);
                }
            }
            return results;
        }

        /**
         * Layout the layer - remember to make the redraw/invalidate call lazy
         */
        @Override
        protected void layoutLayer() {

            Collection<APRSNode> nodes = nodeMap.values();
            for (APRSNode node : nodes) {
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
                    animateNode(existingNode.getIcon());

                }

                // markDirty();
            });
        }

        public void animateNode(Node node) {

            FadeTransition ft = new FadeTransition();
            ft.setNode(node);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setDuration(javafx.util.Duration.millis(500));
            ft.setCycleCount(8);
            ft.setAutoReverse(true);
            ft.play();
        }

        public void updateNode(APRSNode node) {
            MapPoint location = node.getLocation();

            // Get the icon
            Node icon = node.getIcon();
            Point2D mapPoint = getMapPoint(location.getLatitude(), location.getLongitude());
            icon.setVisible(true);
            icon.setTranslateX(mapPoint.getX() - (16));
            icon.setTranslateY(mapPoint.getY() - 16);

//            // Rotate the node to show it updated
//            RotateTransition rt = new RotateTransition();
//            rt.setNode(icon);
//            rt.setByAngle(360);
//            rt.setDuration(javafx.util.Duration.millis(1000));
//            rt.setCycleCount(1);
//            rt.setAutoReverse(false);
//            rt.play();

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
            node.setTranslateX(mapPoint.getX() - (16));
            node.setTranslateY(mapPoint.getY() - 16);

            // Tooltip for useful information popup
            Tooltip t = new Tooltip(aprsNode.getSourceCallsign());
            Tooltip.install(node, t);

            // Probably add our context sensitive mentu here too

            // Initial opacity is 0 as we are fading in
            node.setOpacity(0);

            // Add the node to the GUI
            getChildren().add(node);

            // Fade the node into visibility
            FadeTransition ft = new FadeTransition();
            ft.setNode(node);
            ft.setFromValue(0);
            ft.setToValue(1.0);
            ft.setDuration(javafx.util.Duration.millis(1000));
            ft.setCycleCount(1);
            ft.setAutoReverse(false);
            ft.play();
        }

    }


}