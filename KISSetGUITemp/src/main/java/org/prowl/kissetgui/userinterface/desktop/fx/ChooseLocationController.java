package org.prowl.kissetgui.userinterface.desktop.fx;


import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kissetgui.userinterface.desktop.utils.SymbolCache;
import org.prowl.kisset.util.Tools;
import org.prowl.maps.MapLayer;
import org.prowl.maps.MapPoint;
import org.prowl.maps.MapView;
import org.prowl.maps.Point;

public class ChooseLocationController {

    private static final Log LOG = LogFactory.getLog("ChooseLocationController");
    @FXML
    MapView mapView;
    @FXML
    Label statusLabel;

    private Point2D selectedLocation;
    private Stage stage;
    private Runnable callback;

    @FXML
    private ToolBar toolbar;


    /**
     * This is the map layer that will display aprs locations.
     */
    private ChooseLocationLayer chooseLocationLayer;


    /**
     * Called when a user types a locator into the locator box
     */
    @FXML
    public void onLocatorTyped() {

    }

    @FXML
    public void onOK() {
        callback.run();
        stage.close();
    }



    /**
     * Setup the map
     */
    public void setup(Stage stage, Runnable callback) {
        this.stage = stage;
        this.callback = callback;

        stage.setOnCloseRequest(event -> {
            try {
                callback.run();
            } catch (Throwable e) {
                LOG.debug(e.getMessage(), e);
            }
            stage.close();
        });

        mapView.setZoom(2);
        chooseLocationLayer = new ChooseLocationLayer();
        mapView.addLayer(chooseLocationLayer);




        mapView.setOnMouseClicked(event -> {


            LOG.debug(event);
//                // Check it's actually a click
//                if (event.getClickCount() != 1) {
//                    return;
//                }


            MapPoint location = mapView.getMapPosition(event.getSceneX(), event.getSceneY()-toolbar.getHeight());
            chooseLocationLayer.setLocation(location);
            statusLabel.setText("Locator: " + Tools.toLocator(location.getLatitude(), location.getLongitude())+",  Lat/Lon:"+location.getLatitude() + ", " + location.getLongitude());
            chooseLocationLayer.setPoint(location);
            event.consume();
        });

    }

    public MapPoint getLocation() {
        return chooseLocationLayer.getLocation();
    }

    public class ChooseLocationLayer extends MapLayer {

        private ImageView node;
        private MapPoint location;

        public ChooseLocationLayer() {
            super();

            node = new ImageView();
            node.setImage(SymbolCache.getSymbol('\\', 'o'));
        }

        public MapPoint getLocation() {
            return location;
        }

        public void clear() {
            getChildren().clear();
        }

        public void setLocation(MapPoint location) {
            this.location = location;
        }


        public void setPoint(MapPoint updatedLocation) {
            this.location = location;
            Point mapPoint = new Point();
            getChildren().clear();
            getChildren().add(node);
            getMapPoint(mapPoint, updatedLocation.getLatitude(), updatedLocation.getLongitude());
            node.setVisible(true);
            node.setTranslateX(mapPoint.x - (16));
            node.setTranslateY(mapPoint.y - 16);

        }


        /**
         * Layout the layer - remember to make the redraw/invalidate call lazy
         */
        @Override
        protected void layoutLayer() {
            if (node != null && location != null) {
                Point mapPoint = new Point();
                getMapPoint(mapPoint, location.getLatitude(), location.getLongitude());
                node.setTranslateX(mapPoint.x - (16));
                node.setTranslateY(mapPoint.y - 16);
            }
        }





    }


}