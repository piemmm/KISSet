package org.prowl.kisset.comms.fx;

import com.kodedu.terminalfx.TerminalView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.prowl.kisset.KISSet;

public class KISSetController {

    @FXML
    TextField textEntry;

    @FXML
    TerminalView terminalView;



    @FXML
    protected void onQuitAction() {
        // Ask the user if they really want to quit?

        // Quit the application
        KISSet.INSTANCE.quit();
    }

    @FXML
    protected void onTextEnteredAction(ActionEvent event) {

        System.out.println("moo"+textEntry.getText());
    }

}