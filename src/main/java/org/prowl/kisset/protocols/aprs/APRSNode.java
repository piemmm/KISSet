package org.prowl.kisset.protocols.aprs;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import net.ab0oo.aprs.parser.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.fx.APRSController;


import javax.imageio.ImageIO;
import javax.sound.midi.Track;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is a mapping node that contains information about APRS
 */
public class APRSNode {

    private static final Log LOG = LogFactory.getLog("APRSNode");


    private TrackShape trackLine;

    private String sourceCallsign;
    private String destinationAPRSCallsign;

    private MapPoint location;
    private Node icon;

    public APRSNode(APRSPacket packet) {

        InformationField info = packet.getAprsInformation();
        if (info != null) {
            PositionField positionField = (PositionField) info.getAprsData(APRSTypes.T_POSITION);
            if (positionField != null) {
                Position position = positionField.getPosition();
                location = new MapPoint(position.getLatitude(), position.getLongitude());
                icon = getIconFromTable(position.getSymbolTable(), position.getSymbolCode());
            }
        }
        sourceCallsign = packet.getSourceCall();
        destinationAPRSCallsign = packet.getDestinationCall();

        if (icon == null) {
            icon = new Circle(5, Color.BLACK);
        }
    }

    public String getSourceCallsign() {
        return sourceCallsign;
    }

    public String getDestinationAPRSCallsign() {
        return destinationAPRSCallsign;
    }

    public MapPoint getLocation() {
        return location;
    }

    public void updateLocation(long time, MapPoint newLocation, APRSController.APRSLayer layer) {
        if (location != null) {
            if (trackLine == null) {
                // Lazy create the track line
                trackLine = new TrackShape(layer);
                trackLine.setStroke(Color.RED);
                trackLine.setStrokeWidth(4);
            }
            if (newLocation.getLongitude() != location.getLongitude() || newLocation.getLatitude() != location.getLatitude()) {
                // Only add a point if the location has changed.
                trackLine.addPoint(newLocation);
            }
        }
        this.location = newLocation;

    }

    public Node getIcon() {
        return icon;
    }

    public Node getIconFromTable(char table, char code) {
        return new ImageView(SymbolCache.getSymbol(table, code));
    }

    public TrackShape getTrack() {
        return trackLine;
    }
}
