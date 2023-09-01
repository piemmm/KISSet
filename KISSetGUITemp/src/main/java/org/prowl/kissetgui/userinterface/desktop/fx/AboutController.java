package org.prowl.kissetgui.userinterface.desktop.fx;

import javafx.fxml.FXML;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.prowl.kisset.KISSet;

public class AboutController {

    @FXML
    WebView aboutWebView;

    public void setup() {
        WebEngine engine = aboutWebView.getEngine();
        aboutWebView.setPageFill(Color.TRANSPARENT);


        String version = KISSet.INSTANCE.getVersion();
        version = version.replace("\n", "<br/>");
        engine.loadContent("<html><style>" +
                "img {\n" +
                "  image-rendering: smooth;\n" +
                "  display: block;\n" +
                "  max-width:128;\n" +
                "  max-height:128px;\n" +
                "  width: auto;\n" +
                "  height: auto;\n" +
                "}</style><body style=\"background-color: transparent; color: #888888; font-family: sans-serif; font-size: 14px;\">" +
                "<div align=\"center\"><h1><img width='96' height='96' src=\"" + getClass().getResource("about/app-icon.png") + "\"><br />KISSet</h1>" +
                "<p>Author: Ian Hawkins G0TAI <a href=\"http://prowl.org/\">Web</a></p>" +
                "<p>Build Information:<br/> " + version + "</p>" +

                "</div></body></html>");

    }

}
