package org.prowl.kisset.protocols.aprs;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.aprslib.parser.*;
import org.prowl.kisset.fx.APRSController;
import org.prowl.maps.MapPoint;

/**
 * This is a mapping node that contains information about APRS
 */
public class APRSNode {

    private static final Log LOG = LogFactory.getLog("APRSNode");

    private boolean isAddedToParent = false;
    private TrackShape trackLine;

    private final String sourceCallsign;
    private final String destinationAPRSCallsign;

    private MapPoint location;
    private Node icon;

    public double x;
    public double y;

    public APRSNode(APRSPacket packet) {

        InformationField info = packet.getAprsInformation();
        if (info != null) {
            PositionField positionField = (PositionField) info.getAprsData(APRSTypes.T_POSITION);
            if (positionField != null) {
                Position position = positionField.getPosition();
                location = new MapPoint(position.getLatitude(), position.getLongitude());
                icon = getIconFromTable(position.getSymbolTable(), position.getSymbolCode());
                icon.setCache(false);
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
                trackLine.addPoint(this, newLocation);
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

    public boolean isAddedToParent() {
        return isAddedToParent;
    }

    public void setAddedToParent(boolean isAddedToParent) {
        this.isAddedToParent = isAddedToParent;
    }
}
