package org.prowl.kisset.protocols.aprs;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polyline;
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
