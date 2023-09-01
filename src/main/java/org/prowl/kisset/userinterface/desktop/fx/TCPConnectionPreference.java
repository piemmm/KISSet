package org.prowl.kisset.userinterface.desktop.fx;


import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.kisset.config.BeaconType;


public class TCPConnectionPreference extends ConnectionPreferenceInterface {

    @FXML
    public TextField ipAddressTextField;

    @FXML
    public TextField portTextField;
    @FXML
    private ChoiceBox<BeaconType> beaconChoice;
    @FXML
    private TextField beaconText;

    private PreferencesController preferencesController;
    private HierarchicalConfiguration configInterfaceNode;
    private ConnectionPreferenceHost connectionPreferenceHost;

    @Override
    public boolean validate() {
        try {
            String address = ipAddressTextField.getText();
            if (address.length() < 2) {
                return false;
            }

            String port = portTextField.getText();
            int intport = Integer.parseInt(port);
            if (intport == 0 || intport > 65534) {
                return false;
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    @FXML
    private void onIPChanged() {
        connectionPreferenceHost.setValidation(validate());
    }

    @FXML
    private void onPortChanged() {
        connectionPreferenceHost.setValidation(validate());
    }


    @Override
    public void init(HierarchicalConfiguration configInterfaceNode, PreferencesController preferencesController, ConnectionPreferenceHost host) {
        super.init(configInterfaceNode, preferencesController, host);
        this.preferencesController = preferencesController;
        this.configInterfaceNode = configInterfaceNode;
        this.connectionPreferenceHost = host;

        // Nothing to do if no configuration
        if (configInterfaceNode == null) {
            return;
        }
        ipAddressTextField.setText(configInterfaceNode.getString("ipAddress"));
        portTextField.setText(configInterfaceNode.getString("port"));
        connectionPreferenceHost.setValidation(validate());
        super.applyFromConfig(configInterfaceNode);
    }

    protected void applyToConfig(HierarchicalConfiguration configuration) {
        configuration.setProperty("ipAddress", ipAddressTextField.getText());
        configuration.setProperty("port", portTextField.getText());
        super.applyToConfig(configuration);
    }


}
