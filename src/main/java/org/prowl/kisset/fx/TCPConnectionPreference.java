package org.prowl.kisset.fx;


import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.apache.commons.configuration.HierarchicalConfiguration;


public class TCPConnectionPreference implements ConnectionPreferenceInterface {

    @FXML
    public TextField ipAddressTextField;

    @FXML
    public TextField portTextField;

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

    public void applyToConfig(HierarchicalConfiguration configuration) {
        configuration.setProperty("ipAddress", ipAddressTextField.getText());
        configuration.setProperty("port", portTextField.getText());
    }
}
