package org.prowl.kisset.fx;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.layout.Pane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.InterfaceDriver;
import org.prowl.kisset.io.Interface;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.Set;

public class ConnectionPreferenceHost {


    @FXML
    private ComboBox interfaceType;

    @FXML
    private Pane connectionContent;

    @FXML
    private Button okButton;

    @FXML
    private Button cancelButton;

    @FXML
    private void onInterfaceTypeSelected(ActionEvent event) {
        Class interfaceClass = (Class) interfaceType.getSelectionModel().getSelectedItem();
        showPanelForInterfaceType(interfaceClass);
    }

    @FXML
    private void onOKAction() {

    }

    @FXML
    private void onCancelAction() {

    }

    private static final Log LOG = LogFactory.getLog("ConnectionPreferenceHost");

    private static final Set<Class<?>> ALL_TYPES = new Reflections("org.prowl.kisset.io").getTypesAnnotatedWith(InterfaceDriver.class);


    /**
     * Setup the controls - if null is passed in then this a new interface.
     *
     * @param interfaceToEdit
     */
    public void setup(Interface interfaceToEdit, ) {
        setupDriverConfigPane(interfaceToEdit);
    }

    public void setupDriverConfigPane(Interface anInterface) {


        interfaceType.setCellFactory(param -> new ComboBoxListCell<Class>() {
            @Override
            public void updateItem(Class item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    String name = ((InterfaceDriver) item.getAnnotation(InterfaceDriver.class)).name();
                    setText(name);
                }
            }
        });
        interfaceType.setButtonCell(new ComboBoxListCell<Class>() {
            @Override
            public void updateItem(Class item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    String name = ((InterfaceDriver) item.getAnnotation(InterfaceDriver.class)).name();
                    setText(name);
                }
            }
        });
        interfaceType.setItems(FXCollections.observableArrayList(ALL_TYPES));

        Class intClass = null;
        if (anInterface != null) {
            intClass = anInterface.getClass();
        }

        showPanelForInterfaceType(intClass);
    }

    public void showPanelForInterfaceType(Class anInterface) {


        // Nothing selected? remove the children
        if (anInterface == null) {
            connectionContent.getChildren().clear();
            return;
        }

        // Get the annotation
        InterfaceDriver interfaceDriver = (InterfaceDriver) anInterface.getAnnotation(InterfaceDriver.class);
        String uiName = interfaceDriver.uiName();

        try {

            FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource(uiName));
            Parent root = fxmlLoader.load();
            // ConnectionPreferenceHost controller = fxmlLoader.getController();
            connectionContent.getChildren().add(root);

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }


    }

    public void validateConfig() {

    }

}
