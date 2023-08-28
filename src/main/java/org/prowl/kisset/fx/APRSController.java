package org.prowl.kisset.fx;


import com.google.common.eventbus.Subscribe;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.APRSPacketEvent;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.protocols.aprs.APRSNode;
import org.prowl.kisset.protocols.aprs.TrackShape;
import org.prowl.kisset.util.Tools;
import org.prowl.maps.MapLayer;
import org.prowl.maps.MapPoint;
import org.prowl.maps.MapView;

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


    private List<APRSNode> visibleNodes = new ArrayList<>();

    @Subscribe
    public void onConfigChanged(ConfigurationChangedEvent event) {

    }

    public final Object MONITOR = new Object();

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

        private Bounds bounds;

        public APRSLayer() {
            super();
            SingleThreadBus.INSTANCE.register(this);

            Tools.runOnThread(() -> {
                while (true) {
                    synchronized (MONITOR) {
                        try {
                            MONITOR.wait();
                        } catch (Throwable e) {
                        }
                    }
                    if (bounds == null) {
                        continue;
                    }
                    List<APRSNode> newNodes = new ArrayList<>();
                    synchronized (nodeMap) {
                        for (APRSNode node : nodeMap.values()) {

                            Point2D mapPoint = getMapPoint(node.getLocation().getLatitude(), node.getLocation().getLongitude());
                            node.x = mapPoint.getX() - (16);
                            node.y = mapPoint.getY() - 16;

                            if (hideIfInvisible(node)) {
                                newNodes.add(node);
                            }
                        }
                    }
                    visibleNodes = newNodes;
                }
            });

        }

        public void clear() {
            nodeMap.clear();
            visibleNodes.clear();
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
            bounds = mapView.getLayoutBounds();

            synchronized (MONITOR) {
                MONITOR.notifyAll();
            }

            for (APRSNode node : visibleNodes) {
                updateNode(node);
                //if (hideIfInvisible(node)) {
                updateTrack(node);
                //}
            }
        }

        public boolean hideIfInvisible(APRSNode node) {

            Node icon = node.getIcon();
            if (bounds.contains(node.x, node.y)) {
                if (!node.isAddedToParent()) {
                    node.setAddedToParent(true);
                    //    node.getIcon().setVisible(true);
                    Platform.runLater(() -> {
                        updateNode(node);
                        aprsLayer.getChildren().add(icon);
                        if (node.getTrack() != null) {
                            aprsLayer.getChildren().add(node.getTrack());
                        }
                    });
                }
            } else {
                if (node.isAddedToParent()) {
                    node.setAddedToParent(false);
                    //node.getIcon().setVisible(false);
                    Platform.runLater(() -> {
                        aprsLayer.getChildren().remove(icon);
                        if (node.getTrack() != null) {
                            aprsLayer.getChildren().remove(node.getTrack());
                        }
                    });
                }
            }
            return node.isAddedToParent();
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

                // Get the current viewing rectangle


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
                        //getChildren().add(existingNode.getTrack());
                    }
                    updateNode(existingNode);
                    animateNode(existingNode.getIcon());

                }

                // markDirty();
            });
        }

        public void animateNode(Node node) {

//            FadeTransition ft = new FadeTransition();
//            ft.setNode(node);
//            ft.setFromValue(1.0);
//            ft.setToValue(0.0);
//            ft.setDuration(javafx.util.Duration.millis(500));
//            ft.setCycleCount(8);
//            ft.setAutoReverse(true);
//            ft.play();

            RotateTransition rt = new RotateTransition();
            rt.setNode(node);
           rt.setByAngle(360);
            rt.setAxis(Rotate.Y_AXIS);
            rt.setDuration(javafx.util.Duration.millis(500));
            rt.setCycleCount(4);
            rt.setAutoReverse(false);
            rt.play();
        }

        public void updateNode(APRSNode node) {
            MapPoint location = node.getLocation();

            // Get the icon
            Node icon = node.getIcon();
            Point2D mapPoint = getMapPoint(location.getLatitude(), location.getLongitude());
            icon.setTranslateX(mapPoint.getX() - (16));
            icon.setTranslateY(mapPoint.getY() - 16);
            node.x = mapPoint.getX() - (16);
            node.y = mapPoint.getY() - 16;

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

            //  Probably add our context sensitive mentu here too


            // Add the node to the map if in the visible area
            if (bounds == null) {
                return;
            }
            if (hideIfInvisible(aprsNode)) {
                node.setOpacity(0);
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


}