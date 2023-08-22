package org.prowl.kisset.protocols.aprs;

import com.gluonhq.maps.MapPoint;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import net.ab0oo.aprs.parser.*;


import javax.swing.*;

/**
 * This is a mapping node that contains information about APRS
 */
public class APRSNode {

    private String sourceCallsign;
    private String destinationAPRSCallsign;

    private MapPoint location;
    private Node icon;

    public APRSNode(APRSPacket packet) {

        InformationField info = packet.getAprsInformation();
        if (info != null) {

            PositionField positionField = (PositionField)info.getAprsData(APRSTypes.T_POSITION);
            if (positionField != null ){
                Position position = positionField.getPosition();
                location = new MapPoint(position.getLatitude(), position.getLongitude());
            }




        }

        sourceCallsign = packet.getSourceCall();
        destinationAPRSCallsign = packet.getDestinationCall();

        icon = new Circle(5, Color.RED);

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


    public Node getIcon() {
        return icon;
    }


}
