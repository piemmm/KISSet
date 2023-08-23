package org.prowl.kisset.protocols.aprs;

import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SymbolCache {

    private static final Log LOG = LogFactory.getLog("SymbolCache");

    private static Image primary = new Image(KISSet.class.getResourceAsStream("img/aprs0.png"));
    private static Image secondary = new Image(KISSet.class.getResourceAsStream("img/aprs1.png"));

    private static Map<Integer, Image> symbolCache = Collections.synchronizedMap(new HashMap<>());

    public static Image getSymbol(char table, char code) {

        int key = ((table & 0xFFFF) * 255)+(code & 0xFFFF);

        Image image = symbolCache.get(key);
        if (image != null) {
            return image;
        }

        Image tableImage = null;
        if (table == '/') {
            tableImage = primary;
        } else if (table == '\\') {
            tableImage = secondary;
        } else {
            return null;
        }

        LOG.debug("table=" + table + ", code=" + code+"   "+key);

        int iconSize = (int) (tableImage.getWidth() / 16d);

        int x = (((int) code) - 33) % 16;
        int y = (code-33) / 16;

        x = x * iconSize;
        y = y * iconSize;

        Canvas canvas = new Canvas(iconSize, iconSize);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.drawImage(tableImage, x, y, iconSize, iconSize, 0, 0, 32, 32);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0,0,32,32));
        image = canvas.snapshot(params, null);
        symbolCache.put(key, image);
        return image;



    }

}
