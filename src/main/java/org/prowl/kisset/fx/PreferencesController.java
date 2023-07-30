package org.prowl.kisset.fx;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.stage.Stage;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.io.Interface;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class PreferencesController {

    private static final Log LOG = LogFactory.getLog("PreferencesController");
    @FXML
    private Button cancelButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button addInterfaceButton;
    @FXML
    private Button removeInterfaceButton;
    @FXML
    private Button editInterfaceButton;
    @FXML
    private Label explanatoryText;
    @FXML
    private ListView interfaceList;
    private final List<Interface> interfaces = new ArrayList<>();
    private Config config;

    @FXML
    public void onCancelButtonClicked() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    @FXML
    public void onSaveButtonClicked() {
        // Save our preferences config
        config.saveConfig();

        // Tell the main app to reload the new config
        KISSet.INSTANCE.initAll();

        // Probably post a configChanged event here.
        ((Stage) saveButton.getScene().getWindow()).close();
    }

    /**
     * Return the configuration that we are modifying.
     *
     * @return
     */
    Config getPreferencesConfiguration() {
        return config;
    }

    @FXML
    public void addInterfaceButtonClicked() {
        showAddInterfaceScreen(null);
    }

    @FXML
    public void removeInterfaceButtonClicked() {
        removeInterface((Interface) interfaceList.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void editInterfaceButtonClicked() {
        showAddInterfaceScreen((Interface) interfaceList.getSelectionModel().getSelectedItem());
    }

    public void setup() {

        config = new Config(); // Load a new config we can modify without comitting.

        editInterfaceButton.setDisable(true);
        removeInterfaceButton.setDisable(true);

        interfaceList.setCellFactory(listView -> {
            return new ComboBoxListCell<Interface>() {
                @Override
                public void updateItem(Interface item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText(item.toString());
                    }
                }
            };
        });


        updateList();
    }

    public void updateList() {
        interfaces.clear();
        // Get a list of interfaces
        List<HierarchicalConfiguration> interfaceConfigs = config.getConfig("interfaces").configurationsAt("interface");
        for (HierarchicalConfiguration interfaceConfig : interfaceConfigs) {
            String className = interfaceConfig.getString("className");
            try {
                Class<?> interfaceClass = Class.forName(className);
                Constructor<?> constructor = interfaceClass.getConstructor(HierarchicalConfiguration.class);
                Interface interfaceInstance = (Interface) constructor.newInstance(interfaceConfig);
                //   Interface interfaceInstance = (Interface) interfaceClass.newInstance();
                interfaces.add(interfaceInstance);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        // Populate the list view from the interface list.
        ObservableList<Interface> interfaceItems = FXCollections.observableArrayList(interfaces);
        interfaceList.setItems(interfaceItems);
        interfaceList.getSelectionModel().selectedItemProperty().addListener((Observable observable) -> {
            Interface selectedInterface = (Interface) interfaceList.getSelectionModel().getSelectedItem();
            if (selectedInterface != null) {
                editInterfaceButton.setDisable(false);
                removeInterfaceButton.setDisable(false);
            } else {
                editInterfaceButton.setDisable(true);
                removeInterfaceButton.setDisable(true);
            }
        });


    }


    public void showAddInterfaceScreen(Interface interfaceToEditOrNull) {
        try {
            // Create the GUI.
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(KISSet.class.getResource("fx/ConnectionPreferenceHost.fxml"));
            Parent root = fxmlLoader.load();
            ConnectionPreferenceHost controller = fxmlLoader.getController();
            Scene scene = new Scene(root, 640, 480);
            stage.setTitle("KISS interface settings");
            stage.setScene(scene);
            stage.show();
            controller.setup(interfaceToEditOrNull, this);
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void removeInterface(Interface interfaceToRemove) {
        interfaces.remove(interfaceToRemove);
        interfaceList.getItems().remove(interfaceToRemove);

        String uuid = interfaceToRemove.getUUID();
        LOG.info("uuid: " + uuid);

        HierarchicalConfiguration interfacesNode = config.getConfig("interfaces");
        // Get a list of all interfaces
        List<HierarchicalConfiguration> interfaceList = interfacesNode.configurationsAt("interface");
        // Get the one with the correct UUID
        for (HierarchicalConfiguration interfaceNode : interfaceList) {
            if (interfaceNode.getString("uuid").equals(uuid)) {
                // Remove the interface node from the interfaces node.
                config.getConfig("interfaces").getRootNode().removeChild(interfaceNode.getRootNode());
                break;
            }
        }
//


    }
}
