package org.prowl.kisset.protocols.aprs;

import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SymbolCache {

    private static final Log LOG = LogFactory.getLog("SymbolCache");

    private static final Image primary = new Image(KISSet.class.getResourceAsStream("img/aprs0.png"));
    private static final Image alternate = new Image(KISSet.class.getResourceAsStream("img/aprs1.png"));

    private static final Map<Integer, Image> symbolCache = Collections.synchronizedMap(new HashMap<>());

    public static Image getSymbol(char table, char code) {

        int key = ((table & 0xFFFF) * 255) + (code & 0xFFFF);

        Image image = symbolCache.get(key);
        if (image != null) {
            return image;
        }

        Image tableImage = null;
        if (table == '/') {
            tableImage = primary;
        } else if (table == '\\' || (table >= 'A' && table <= 'Z') || (table >= '0' && table <= '9')) {
            tableImage = alternate;
        } else {
            return null;
        }

        int iconSize = (int) (tableImage.getWidth() / 16d);

        int x = (((int) code) - 33) % 16;
        int y = (code - 33) / 16;

        x = x * iconSize;
        y = y * iconSize;
//
//        DropShadow dropShadow = new DropShadow();
//        dropShadow.setRadius(3.0);
//        dropShadow.setOffsetX(3.0);
//        dropShadow.setOffsetY(3.0);
//        dropShadow.setSpread(0);


        Canvas canvas = new Canvas(32, 32);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        //gc.setEffect(dropShadow);

        gc.setImageSmoothing(true);
        gc.drawImage(tableImage, x, y, iconSize, iconSize, 0, 0, 32, 32);

        // If table is A-Z or 0-9, draw the table letter/number over the image in the centre.
        // with white text, but a black stroke.
        if ((table >= 'A' && table <= 'Z') || (table >= '0' && table <= '9')){
            gc.setFill(Color.WHITE);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2.25);
            Font f = gc.getFont();
            gc.setFont(Font.font(f.getFamily(), FontWeight.BOLD, 14));

            String overlayTextCharacter = String.valueOf(table);
            Text text = new Text(overlayTextCharacter);
            text.setFont(gc.getFont());
            // Get the font metrics so we can properly centre the text
            double w = text.getBoundsInLocal().getWidth();
            double h = text.getBoundsInLocal().getHeight();
            double hb = h-text.getBaselineOffset();

            gc.strokeText(String.valueOf(table), (32d/2d)-(w/2d),  (16+(h/2d))-hb);

            gc.setLineWidth(1);
            gc.setStroke(Color.WHITE);
            gc.strokeText(String.valueOf(table), (32d/2d)-(w/2d),  (16+(h/2d))-hb);

            gc.fillText(String.valueOf(table), (32d/2d)-(w/2d), (16+(h/2d))-hb);
        }


        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0, 0, 32, 32));
        image = canvas.snapshot(params, null);
        symbolCache.put(key, image);
        return image;


    }

}
