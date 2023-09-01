package org.prowl.kissetgui.guiconfig;


import javafx.scene.text.Font;
import org.controlsfx.tools.Platform;

import java.util.List;

public class OSDefaults {


    public static final String getDefaultPlatformFont() {

        List<String> fontFamilies = Font.getFamilies();
        List<String> fontNames = Font.getFontNames();


        String font = "Monospace";

        Platform current = Platform.getCurrent();
        if (current.equals(Platform.WINDOWS)) {
            font = "Consolas";
        } else if (current.equals(Platform.OSX)) {
            font = "Monaco";
        } else if (current.equals(Platform.UNIX)) {
            if (fontFamilies.contains("Hack")) {
                font = "Hack";
            } else {
                font = "Monospace";
            }
        }

        return font;
    }


}
