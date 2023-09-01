package org.prowl.kisset.protocols.aprs;

import javafx.collections.ObservableList;
import javafx.scene.shape.Polyline;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.Tools;
import org.prowl.maps.MapPoint;
import org.prowl.kisset.userinterface.desktop.fx.APRSController;
import org.prowl.maps.Point;

import java.util.ArrayList;
import java.util.List;

public final class TrackShape extends Polyline {

    private static final Log LOG = LogFactory.getLog("TrackShape");

    private final APRSController.APRSLayer mapLayer;

    private final List<MapPoint> points = new ArrayList<>();
    private final Point sharedUpdatePoint = new Point();

    public TrackShape(APRSController.APRSLayer layer) {
        super();
        this.mapLayer = layer;
    }

    public void addPoint(APRSNode node, MapPoint point) {

        // Get the last point
        if (point.getLatitude() == 0 || point.getLongitude() == 0) {
            return; // Ignore if either are 0 (not just both), we accept this given the amount of corrupt entries
            // with one or more of these as 0.
        }

        //Now check the distance isn't stupidly large
        if (points.size() > 0) {
            MapPoint lastPoint = points.get(points.size() - 1);
            double distance =  Tools.distance(point.getLatitude(), lastPoint.getLatitude(), point.getLongitude(), lastPoint.getLongitude(), 0,0);
            if (distance > 20000) {
                return; // Ignore if the distance is more than 100km, this is probably a corrupt entry
            }
            // We also ignore stationary GPS aliasing meanders.
            if (distance < 20) {
              //  LOG.debug("Ignoring due to distance: " + distance);
                return;
            }
        }
        Point p = new Point();
        mapLayer.getMapPointExt(p, point.getLatitude(), point.getLongitude());
        super.getPoints().add(p.x);
        super.getPoints().add(p.y);
        points.add(point);

        // We cap at 100 points max.
        if (points.size() > 100) {
            points.remove(0);
        }

        if (points.size() > 70) {
            LOG.debug("Points list size: " + points.size() + " for aprs: " + node.getSourceCallsign() + " at " + point.getLatitude() + ", " + point.getLongitude());
        }
    }

    public void update() {
        ObservableList<Double> pointsList = getPoints();
        synchronized (pointsList) {
            pointsList.clear();
            for (MapPoint point : points) {
                mapLayer.getMapPointExt(sharedUpdatePoint, point.getLatitude(), point.getLongitude());
                pointsList.add(sharedUpdatePoint.x);
                pointsList.add(sharedUpdatePoint.y);
            }
        }
    }


}
