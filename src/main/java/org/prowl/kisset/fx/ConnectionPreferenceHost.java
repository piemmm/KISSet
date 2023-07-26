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
import javafx.stage.Stage;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.annotations.InterfaceDriver;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.io.Interface;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        // Tell the pane to write its config to our current preferences config
        connectionPreferenceClient.applyToConfig(configInterfaceNode);
        applyToConfig(configInterfaceNode);
        preferencesController.updateList(); // lazy.

        // Close the window
        ((Stage)okButton.getScene().getWindow()).close();

    }

    @FXML
    private void onCancelAction() {
        // Just close the window - no need to do anything else
        ((Stage)cancelButton.getScene().getWindow()).close();

    }

    private static final Log LOG = LogFactory.getLog("ConnectionPreferenceHost");

    private static final Set<Class<?>> ALL_TYPES = new Reflections("org.prowl.kisset.io").getTypesAnnotatedWith(InterfaceDriver.class);

    private PreferencesController preferencesController;

    private HierarchicalConfiguration configInterfaceNode;

    private ConnectionPreferenceInterface connectionPreferenceClient;

    private Class currentInterfaceClass;

    /**
     * Setup the controls - if null is passed in then this a new interface.
     *
     * @param interfaceToEdit
     */
    public void setup(Interface interfaceToEdit, PreferencesController preferencesController) {
        this.preferencesController = preferencesController;

        HierarchicalConfiguration interfacesNode = preferencesController.getPreferencesConfiguration().getConfig("interfaces");

        // If passed in an interafce, then get the correct config.
        if (interfaceToEdit != null) {
            // Get a list of all interfaces
            List<HierarchicalConfiguration> interfaceList = interfacesNode.configurationsAt("interface");
            // Get the one with the correct UUID
            for (HierarchicalConfiguration interfaceNode : interfaceList) {
                if (interfaceNode.getString("uuid").equals(interfaceToEdit.getUUID().toString())) {
                    configInterfaceNode = interfaceNode;
                    break;
                }
            }
        } else {
            // Create a new interface in the interfaces list which contains many interface tags (one for each interface)
            HierarchicalConfiguration.Node node = new HierarchicalConfiguration.Node("interface");
            HierarchicalConfiguration.Node nodeUUID = new HierarchicalConfiguration.Node("uuid", UUID.randomUUID().toString());
            node.addChild(nodeUUID);
            ArrayList<HierarchicalConfiguration.Node> nodeList = new ArrayList<>();
            nodeList.add(node);
            interfacesNode.addNodes("", nodeList);
            List<HierarchicalConfiguration> iConfigs = interfacesNode.configurationsAt("interface");
            configInterfaceNode = iConfigs.get(iConfigs.size() - 1); // This is a bit hacky, but works.
        }

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
            // Select current item in model
            interfaceType.getSelectionModel().select(intClass);
        }

        showPanelForInterfaceType(intClass);
    }

    public void showPanelForInterfaceType(Class anInterface) {
        currentInterfaceClass = anInterface;

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

            // Get the controller we are using.
            connectionPreferenceClient = fxmlLoader.getController();
            connectionPreferenceClient.init(configInterfaceNode, preferencesController);
            connectionContent.getChildren().add(root);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }



    }

    public void applyToConfig(HierarchicalConfiguration configuration) {
        configuration.setProperty("className", currentInterfaceClass.getName());
    }


    public void validateConfig() {

    }

}
