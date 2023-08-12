package org.prowl.kisset.fx;

import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.KISSet;
import org.prowl.kisset.Messages;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.eventbus.SingleThreadBus;
import org.prowl.kisset.eventbus.events.ConfigurationChangedEvent;
import org.prowl.kisset.io.Interface;
import org.prowl.kisset.util.Tools;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
    @FXML
    private TextField stationCallsign;
    @FXML
    private ComboBox fontSelector;
    @FXML
    private ComboBox fontSize;
    @FXML
    private Slider monitorTransparency;
    @FXML
    private Slider terminalTransparency;

    // PMS
    @FXML
    private CheckBox enablePMSCheckbox;
    @FXML
    private ChoiceBox pmsSSIDChoiceBox;
    @FXML
    private TextField pmsGreetingTextField;

    // Net/ROM
    @FXML
    private CheckBox enableNetRomCheckbox;
    @FXML
    private ChoiceBox netromNodeSSIDChoiceBox;
    @FXML
    private TextField netromNodeAliasTextField;
    @FXML
    private TextField netromGreetingTextField;


    // APRS


    private final List<Interface> interfaces = new ArrayList<>();
    private Config config;

    private int[] FONT_SIZES = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};

    private int[] NETROM_SSID = { -1, -2, -3, -4, -5, -6, -7 };

    private int[] MAILBOX_SSID = { -1, -2, -3, -4, -5, -6, -7 };


    @FXML
    public void onCancelButtonClicked() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    @FXML
    public void onSaveButtonClicked() {

        // General
        config.setProperty(Conf.callsign, stationCallsign.getText());

        // Terminal
        config.setProperty(Conf.terminalFont, fontSelector.getSelectionModel().getSelectedItem());
        config.setProperty(Conf.terminalFontSize, fontSize.getSelectionModel().getSelectedItem());

        // Transparency slider for packet monitor
        config.setProperty(Conf.monitorTransparency, (int) monitorTransparency.getValue());

        // Transparency slider for main window
        config.setProperty(Conf.terminalTransparency, (int) terminalTransparency.getValue());

        // PMS
        config.setProperty(Conf.pmsEnabled, enablePMSCheckbox.isSelected());
        config.setProperty(Conf.pmsSSID, pmsSSIDChoiceBox.getSelectionModel().getSelectedItem());
        config.setProperty(Conf.pmsGreetingText, pmsGreetingTextField.getText());

        // Net/ROM
        config.setProperty(Conf.netromEnabled, enableNetRomCheckbox.isSelected());
        config.setProperty(Conf.netromSSID, netromNodeSSIDChoiceBox.getSelectionModel().getSelectedItem());
        config.setProperty(Conf.netromAlias, netromNodeAliasTextField.getText());
        config.setProperty(Conf.netromGreetingText, netromGreetingTextField.getText());

        // Interfaces preference pane
        // Save our preferences config
        config.saveConfig();

        // Probably post a configChanged event here.
        ((Stage) saveButton.getScene().getWindow()).close();

        // Notify anything interested
        SingleThreadBus.INSTANCE.post(new ConfigurationChangedEvent());
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

    @FXML
    public void onFontSelected() {

    }

    public void setup() {

        config = new Config(); // Load a new config we can modify without comitting.

        editInterfaceButton.setDisable(true);
        removeInterfaceButton.setDisable(true);

        updateList();
        updateControls();
    }

    public void updateControls() {
        // General preference pane
        stationCallsign.setText(config.getConfig(Conf.callsign, Conf.callsign.stringDefault()).toUpperCase(Locale.ENGLISH));


        fontSelector.setCellFactory(listView -> {
            return new ComboBoxListCell<String>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setFont(Font.font(item));
                        setText(item);
                    }
                }
            };
        });

        // Terminal preference pane
        Tools.getMonospacedFonts().forEach(fontSelector.getItems()::add);
        for (int fontSizei : FONT_SIZES) {
            fontSize.getItems().add(fontSizei);
        }



        // Monitor window transparency
        monitorTransparency.setValue(config.getConfig(Conf.monitorTransparency, Conf.monitorTransparency.intDefault()));

        // Main terminal window transparency
        terminalTransparency.setValue(config.getConfig(Conf.terminalTransparency, Conf.terminalTransparency.intDefault()));

        // PMS
        //pmsSSIDChoiceBox.getItems().add(
        Arrays.stream(MAILBOX_SSID).forEach(pmsSSIDChoiceBox.getItems()::add);
        enablePMSCheckbox.setSelected(config.getConfig(Conf.pmsEnabled, Conf.pmsEnabled.boolDefault()));
        pmsSSIDChoiceBox.getSelectionModel().select(config.getConfig(Conf.pmsSSID, Conf.pmsSSID.stringDefault()));
        pmsGreetingTextField.setText(config.getConfig(Conf.pmsGreetingText, Conf.pmsGreetingText.stringDefault()));

        // Net/ROM
        Arrays.stream(NETROM_SSID).forEach(netromNodeSSIDChoiceBox.getItems()::add);
        //enableNetRomCheckbox.setSelected(config.getConfig(Conf.netromEnabled, Conf.netromEnabled.boolDefault()));
        netromNodeSSIDChoiceBox.getSelectionModel().select(config.getConfig(Conf.netromSSID, Conf.netromSSID.stringDefault()));
        // Best effort to get a sensible default alias
        String alias = config.getConfig(Conf.netromAlias, Conf.createDefaultNetromAlias());
        if (alias == null || alias.length() == 0) {
            alias = Conf.createDefaultNetromAlias();
        }
        netromNodeAliasTextField.setText(alias);
        netromGreetingTextField.setText(config.getConfig(Conf.netromGreetingText, Conf.netromGreetingText.stringDefault()));

        // Set current font
        fontSelector.getSelectionModel().select(config.getConfig(Conf.terminalFont, Conf.terminalFont.stringDefault()));
        fontSize.getSelectionModel().select(Integer.valueOf(config.getConfig(Conf.terminalFontSize, Conf.terminalFontSize.intDefault())));

        explanatoryText.setText(Messages.get("explanatoryText"));
    }



    public void updateList() {
        // Interfaces preference pane
        interfaces.clear();
        // Get a list of interfaces
        List<HierarchicalConfiguration> interfaceConfigs = config.getConfig("interfaces").configurationsAt("interface");
        for (HierarchicalConfiguration interfaceConfig : interfaceConfigs) {
            String className = interfaceConfig.getString("className");
            String uuid = interfaceConfig.getString("uuid");

            try {
                if (className != null) {
                    Class<?> interfaceClass = Class.forName(className);
                    if (interfaceClass != null) {

                        Constructor<?> constructor = interfaceClass.getConstructor(HierarchicalConfiguration.class);
                        Interface interfaceInstance = (Interface) constructor.newInstance(interfaceConfig);
                        //   Interface interfaceInstance = (Interface) interfaceClass.newInstance();
                        interfaces.add(interfaceInstance);
                    } else {
                        LOG.error("Could not find interface class " + className);
                        removeInterface(uuid);
                    }
                } else {
                    LOG.error("Interface class name was null " + className);
                    removeInterface(uuid);
                }
            } catch (Throwable e) {
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
            stage.initOwner(addInterfaceButton.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setScene(scene);
            controller.setup(interfaceToEditOrNull, this);
            stage.showAndWait();
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
    }

    /**
     * Remove a uuid from the config only - this is used for cleanup of any classes that are removed from app.
     *
     * @param uuid
     */
    public void removeInterface(String uuid) {
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
    }
}
