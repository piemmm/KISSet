package org.prowl.kisset.protocols.aprs;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polyline;
import org.prowl.kisset.util.Tools;
import org.prowl.maps.MapPoint;
import org.prowl.kisset.fx.APRSController;

import java.util.ArrayList;
import java.util.List;

public class TrackShape extends Polyline {

    private final APRSController.APRSLayer mapLayer;

    private final List<MapPoint> points = new ArrayList<>();

    public TrackShape(APRSController.APRSLayer layer) {
        super();
        this.mapLayer = layer;
    }

    public void addPoint(MapPoint point) {

        // Get the last point
        if (point.getLatitude() == 0 || point.getLongitude() == 0) {
            return; // Ignore if either are 0 (not just both), we accept this given the amount of corrupt entries
            // with one or more of these as 0.
        }

        //Now check the distance isn't stupidly large
        if (points.size() > 0) {
            MapPoint lastPoint = points.get(points.size() - 1);
            double distance =  Tools.distance(point.getLatitude(), point.getLongitude(), lastPoint.getLatitude(), lastPoint.getLongitude(), 0,0);
            if (distance > 100000) {
                return; // Ignore if the distance is more than 100km, this is probably a corrupt entry
            }
        }

        Point2D p = mapLayer.getMapPointExt(point.getLatitude(), point.getLongitude());
        super.getPoints().add(p.getX());
        super.getPoints().add(p.getY());
        points.add(point);
    }

    public void update() {
        ObservableList<Double> pointsList = getPoints();
        synchronized (pointsList) {
            pointsList.clear();
            for (MapPoint point : points) {
                Point2D p = mapLayer.getMapPointExt(point.getLatitude(), point.getLongitude());
                pointsList.add(p.getX());
                pointsList.add(p.getY());
            }
        }
    }


}
