package org.prowl.kisset.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Tools {

    public static final void runOnThread(Runnable runme) {
        Thread t = new Thread(runme);
        t.start();
    }

    /**
     * Used for things like rate limiting, avoiding spinning exception loops chewing CPU etc.
     *
     * @param millis
     */
    public static void delay(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static String byteArrayToHexString(byte[] output) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < output.length; i++) {
            hexString.append(String.format("%02X", output[i]));
            hexString.append(" ");
        }
        return hexString.toString();
    }

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


    public static boolean isValidITUCallsign(String callsignToValidate) {
        return callsignToValidate.matches("\\A\\d?[a-zA-Z]{1,2}\\d{1,4}[a-zA-Z]{1,3}(-\\d+)?\\Z");
    }


}
