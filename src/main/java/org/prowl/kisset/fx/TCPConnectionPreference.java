package org.prowl.kisset.fx;


import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.kisset.io.Interface;


public class TCPConnectionPreference implements ConnectionPreferenceInterface {

    @FXML
    public TextField ipAddressTextField;

    @FXML
    public TextField portTextField;

    private PreferencesController preferencesController;
    private HierarchicalConfiguration configInterfaceNode;

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
        } catch(Throwable e) {
            return false;
        }
        return true;
    }

    @Override
    public void init(HierarchicalConfiguration configInterfaceNode, PreferencesController preferencesController) {
        this.preferencesController = preferencesController;
        this.configInterfaceNode = configInterfaceNode;

        // Nothing to do if no configuration
        if (configInterfaceNode == null) {
            return;
        }
        ipAddressTextField.setText(configInterfaceNode.getString("ipAddress"));
        portTextField.setText(configInterfaceNode.getString("port"));
    }

    public void applyToConfig(HierarchicalConfiguration configuration) {
        configuration.setProperty("ipAddress", ipAddressTextField.getText());
        configuration.setProperty("port", portTextField.getText());
    }
}
