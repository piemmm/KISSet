package org.prowl.kisset.fx;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.kisset.config.BeaconType;
import org.prowl.kisset.config.Conf;

public abstract class ConnectionPreferenceInterface {

    @FXML
    private ChoiceBox<BeaconType> beaconChoice;
    @FXML
    private TextField beaconText;

    private ConnectionPreferenceHost connectionPreferenceHost;

    @FXML
    private void beaconTextChanged() {
        connectionPreferenceHost.setValidation(validate());
    }
    @FXML
    private void beaconIntervalChanged() { connectionPreferenceHost.setValidation(validate()); }


    protected void init(HierarchicalConfiguration configInterfaceNode, PreferencesController controller, ConnectionPreferenceHost host) {
        this.connectionPreferenceHost = host;
    }

    public boolean validate() {
        return true;
    }


    protected void applyToConfig(HierarchicalConfiguration configuration) {
        configuration.setProperty(Conf.beaconText.name(), beaconText.getText());
        configuration.setProperty(Conf.beaconEvery.name(), beaconChoice.getSelectionModel().getSelectedItem().getInterval());
    }

    protected void applyFromConfig(HierarchicalConfiguration configInterfaceNode) {
        // Beacons
        beaconText.setText(configInterfaceNode.getString(Conf.beaconText.name(), Conf.beaconText.stringDefault()));
        beaconChoice.setItems(FXCollections.observableArrayList(BeaconType.values()));
        int beaconInterval = configInterfaceNode.getInteger(Conf.beaconEvery.name(), Conf.beaconEvery.intDefault());
        beaconChoice.getSelectionModel().select(BeaconType.getBeaconType(beaconInterval));
    }
}
