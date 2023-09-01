package org.prowl.kissetgui.userinterface.desktop.utils;

import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class GUITools {

    /**
     * Get a list of all the monospaced fonts available on the system.
     */
    public static List<String> getMonospacedFonts() {
        List<String> found = new ArrayList<>();
        final Text text1 = new Text("i i l i l i l i l");
        final Text text2 = new Text("A B C D E F G H I");
        for (String fontFamilyName : Font.getFamilies()) {
            // Get the font
            Font font = Font.font(fontFamilyName, FontWeight.NORMAL, FontPosture.REGULAR, 12d);
            text1.setFont(font);
            text2.setFont(font);
            if (text1.getLayoutBounds().getWidth() == text2.getLayoutBounds().getWidth()) {
                found.add(fontFamilyName);
            }
        }
        return found;
    }


    /**
     * Convert from a locator to a lat lon centered in the bounding box
     *
     * @return
     */
    public static Point2D locatorToLatLonCentered(String locator) {
        Point2D latlng = new Point2D.Double();
        Rectangle2D box = locatorToBoundingBox(locator);
        latlng.setLocation(box.getCenterX(), box.getCenterY());
        return latlng;
    }

    /**
     * Convert from a locator to a lat/lon bounding box
     */
    public static Rectangle2D locatorToBoundingBox(String locator) {
        Rectangle2D boundingBox = new Rectangle2D.Double();
        locator = locator.toUpperCase();
        double[] bbox = new double[4];
        bbox[0] = (locator.charAt(0) - 'A') * 20 - 180;
        bbox[1] = (locator.charAt(1) - 'A') * 10 - 90;
        bbox[2] = (locator.charAt(2) - '0') * 2;
        bbox[3] = (locator.charAt(3) - '0');

        bbox[0] += (locator.charAt(4) - 'A') * 5.0 / 60;
        bbox[1] += (locator.charAt(5) - 'A') * 2.5 / 60;
        bbox[2] += 2.5 / 60;
        bbox[3] += 5.0 / 60;

        boundingBox.setRect(bbox[0], bbox[1], bbox[2], bbox[3]);

        return boundingBox;
    }

}
