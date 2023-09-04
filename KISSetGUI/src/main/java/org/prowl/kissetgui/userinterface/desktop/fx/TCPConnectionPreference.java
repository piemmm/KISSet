package org.prowl.kissetgui.userinterface.desktop.fx;


import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.kisset.config.BeaconType;
import org.prowl.kisset.config.Conf;


public class TCPConnectionPreference extends ConnectionPreferenceInterface {

    @FXML
    public TextField ipAddressTextField;

    @FXML
    public TextField portTextField;
    @FXML
    private ChoiceBox<BeaconType> beaconChoice;
    @FXML
    private TextField beaconText;
    @FXML
    private TextField txDelayTextField;
    @FXML
    private TextField txTailTextField;
    @FXML
    private TextField persistenceTextField;
    @FXML
    private TextField slotTimeTextField;
    @FXML
    private TextField maxFramesTextField;
    @FXML
    private TextField pacLenTextField;
    @FXML
    private CheckBox ackModeCheckBox;

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
            if (intport <= 0 || intport > 65534) {
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
        ipAddressTextField.setText(configInterfaceNode.getString(Conf.ipAddress.name()));
        portTextField.setText(configInterfaceNode.getInteger(Conf.port.name(), Conf.port.intDefault()).toString());
        connectionPreferenceHost.setValidation(validate());

        txDelayTextField.setText(configInterfaceNode.getInteger(Conf.txDelay.name(), Conf.txDelay.intDefault()).toString());
        txTailTextField.setText(configInterfaceNode.getInteger(Conf.txTail.name(), Conf.txTail.intDefault()).toString());
        persistenceTextField.setText(configInterfaceNode.getInteger(Conf.persistence.name(),Conf.persistence.intDefault()).toString());
        slotTimeTextField.setText(configInterfaceNode.getInteger(Conf.slotTime.name(), Conf.slotTime.intDefault()).toString());
        maxFramesTextField.setText(configInterfaceNode.getInteger(Conf.maxFrames.name(), Conf.maxFrames.intDefault()).toString());
        pacLenTextField.setText(configInterfaceNode.getInteger(Conf.pacLen.name(), Conf.pacLen.intDefault()).toString());
        ackModeCheckBox.setSelected(configInterfaceNode.getBoolean(Conf.ackMode.name(), Conf.ackMode.boolDefault()));

        super.applyFromConfig(configInterfaceNode);
    }

    protected void applyToConfig(HierarchicalConfiguration configuration) {
        configuration.setProperty(Conf.ipAddress.name(), ipAddressTextField.getText());
        configuration.setProperty(Conf.port.name(), portTextField.getText());

        // TNC Settings
        configuration.setProperty(Conf.txDelay.name(), Integer.parseInt(txDelayTextField.getText()));
        configuration.setProperty(Conf.txTail.name(), Integer.parseInt(txTailTextField.getText()));
        configuration.setProperty(Conf.persistence.name(), Integer.parseInt(persistenceTextField.getText()));
        configuration.setProperty(Conf.slotTime.name(), Integer.parseInt(slotTimeTextField.getText()));
        configuration.setProperty(Conf.maxFrames.name(), Integer.parseInt(maxFramesTextField.getText()));
        configuration.setProperty(Conf.pacLen.name(), Integer.parseInt(pacLenTextField.getText()));
        configuration.setProperty(Conf.ackMode.name(), ackModeCheckBox.isSelected());

        super.applyToConfig(configuration);
    }


}
