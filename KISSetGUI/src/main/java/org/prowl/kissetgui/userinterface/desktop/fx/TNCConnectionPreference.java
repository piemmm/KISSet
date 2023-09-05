package org.prowl.kissetgui.userinterface.desktop.fx;

import com.fazecast.jSerialComm.SerialPort;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxListCell;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.config.Conf;
import org.prowl.kisset.io.KISSviaSerial;

import java.util.Arrays;
import java.util.List;

public class TNCConnectionPreference extends ConnectionPreferenceInterface {
    private static final Log LOG = LogFactory.getLog("SerialPortConnectionPreference");

    // A list of common baud rates
    private static final Integer[] BAUD_RATES = {300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200};
    @FXML
    private ComboBox<SerialPort> serialPortComboBox;
    @FXML
    private ComboBox<Integer> baudRateComboBox;
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

    @FXML
    private void serialPortChanged() {
        connectionPreferenceHost.setValidation(validate());
    }
    @FXML
    private void baudRateChanged() {
        connectionPreferenceHost.setValidation(validate());
    }

    private PreferencesController preferencesController;
    private HierarchicalConfiguration configInterfaceNode;
    private ConnectionPreferenceHost connectionPreferenceHost;



    @Override
    public void init(HierarchicalConfiguration configInterfaceNode, PreferencesController preferencesController, ConnectionPreferenceHost host) {
        super.init(configInterfaceNode, preferencesController, host);
        this.preferencesController = preferencesController;
        this.configInterfaceNode = configInterfaceNode;
        this.connectionPreferenceHost = host;

        List<SerialPort> ports = KISSviaSerial.getListOfSerialPorts();
        serialPortComboBox.setItems(FXCollections.observableList(ports));

        List<Integer> baudRates = Arrays.asList(BAUD_RATES);
        baudRateComboBox.setItems(FXCollections.observableList(baudRates));


        // Setup renderer
        serialPortComboBox.setCellFactory(param -> new ComboBoxListCell<SerialPort>() {
            @Override
            public void updateItem(SerialPort item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    String name = item + " (" + item.getSystemPortPath() + ")";
                    setText(name);
                }
            }
        });
        serialPortComboBox.setButtonCell(new ComboBoxListCell<SerialPort>() {
            @Override
            public void updateItem(SerialPort item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    String name = item + " (" + item.getSystemPortPath() + ")";
                    setText(name);
                }
            }
        });

        applyFromConfig(configInterfaceNode);
    }

    protected void applyFromConfig(HierarchicalConfiguration configInterfaceNode) {

        if (configInterfaceNode == null) {
            return;
        }
        String port = configInterfaceNode.getString(Conf.serialPort.name());
        if (port != null) {
            for (SerialPort serialPort : serialPortComboBox.getItems()) {
                if (serialPort.getSystemPortPath().equals(port)) {
                    serialPortComboBox.getSelectionModel().select(serialPort);
                    break;
                }
            }
        }
        Integer baudRate = configInterfaceNode.getInteger(Conf.baudRate.name(), Conf.baudRate.intDefault());
        baudRateComboBox.getSelectionModel().select(baudRate);

        txDelayTextField.setText(configInterfaceNode.getInteger(Conf.txDelay.name(), Conf.txDelay.intDefault()).toString());
        txTailTextField.setText(configInterfaceNode.getInteger(Conf.txTail.name(), Conf.txTail.intDefault()).toString());
        persistenceTextField.setText(configInterfaceNode.getInteger(Conf.persistence.name(),Conf.persistence.intDefault()).toString());
        slotTimeTextField.setText(configInterfaceNode.getInteger(Conf.slotTime.name(), Conf.slotTime.intDefault()).toString());
        maxFramesTextField.setText(configInterfaceNode.getInteger(Conf.maxFrames.name(), Conf.maxFrames.intDefault()).toString());
        pacLenTextField.setText(configInterfaceNode.getInteger(Conf.pacLen.name(), Conf.pacLen.intDefault()).toString());
        ackModeCheckBox.setSelected(configInterfaceNode.getBoolean(Conf.ackMode.name(), Conf.ackMode.boolDefault()));

        super.applyFromConfig(configInterfaceNode);
    }

    @Override
    public void applyToConfig(HierarchicalConfiguration configuration) {
        SerialPort serialPort = serialPortComboBox.getSelectionModel().getSelectedItem();

        configuration.setProperty(Conf.serialPort.name(), serialPort.getSystemPortPath());
        configuration.setProperty(Conf.baudRate.name(), baudRateComboBox.getSelectionModel().getSelectedItem().intValue());

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

    @Override
    public boolean validate() {

        if (serialPortComboBox.getSelectionModel().getSelectedItem() == null) {
            return false;
        }

        if (baudRateComboBox.getSelectionModel().getSelectedItem() == null) {
            return false;
        }

        if (!txDelayTextField.getText().matches("\\d+")) {
            return false;
        }

        if (!txTailTextField.getText().matches("\\d+")) {
            return false;
        }

        if (!persistenceTextField.getText().matches("\\d+")) {
            return false;
        }

        if (!slotTimeTextField.getText().matches("\\d+")) {
            return false;
        }

        if (!maxFramesTextField.getText().matches("\\d+")) {
            return false;
        }

        if (!pacLenTextField.getText().matches("\\d+")) {
            return false;
        }

        return super.validate();
    }

}
