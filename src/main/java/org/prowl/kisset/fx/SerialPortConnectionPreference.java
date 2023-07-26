package org.prowl.kisset.fx;

import com.fazecast.jSerialComm.SerialPort;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.cell.ComboBoxListCell;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.kisset.io.KISSviaSerial;

import java.util.Arrays;
import java.util.List;

public class SerialPortConnectionPreference implements ConnectionPreferenceInterface {

    @FXML
    private ComboBox<SerialPort> serialPortComboBox;

    @FXML
    private ComboBox<Integer> baudRateComboBox;

    @FXML
    private void serialPortChanged() {

    }

    @FXML
    private void baudRateChanged() {

    }

    // A list of common baud rates
    private static final Integer[] BAUD_RATES = {300, 600, 1200, 2400, 4800, 9600, 19200, 38400, 57600, 115200};

    private PreferencesController preferencesController;
    private HierarchicalConfiguration configInterfaceNode;

    @Override
    public void init(HierarchicalConfiguration configInterfaceNode, PreferencesController preferencesController) {
        this.preferencesController = preferencesController;
        this.configInterfaceNode = configInterfaceNode;

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
                    String name = item.toString() + " (" + item.getSystemPortPath() + ")";
                    setText(name);
                }
            }
        });
        serialPortComboBox.setButtonCell(new ComboBoxListCell<SerialPort>() {
            @Override
            public void updateItem(SerialPort item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    String name = item.toString() + " (" + item.getSystemPortPath() + ")";
                    setText(name);
                }
            }
        });

        applyFromConfig(configInterfaceNode);
    }

    private void applyFromConfig(HierarchicalConfiguration configInterfaceNode) {
        if (configInterfaceNode == null) {
            return;
        }
        String port = configInterfaceNode.getString("serialPort");
        if (port != null) {
            for (SerialPort serialPort : serialPortComboBox.getItems()) {
                if (serialPort.getSystemPortPath().equals(port)) {
                    serialPortComboBox.getSelectionModel().select(serialPort);
                    break;
                }
            }
        }
        Integer baudRate = configInterfaceNode.getInteger("baudRate",9600);
        baudRateComboBox.getSelectionModel().select(baudRate);
    }

    @Override
    public void applyToConfig(HierarchicalConfiguration configuration) {
        SerialPort serialPort = (SerialPort) serialPortComboBox.getSelectionModel().getSelectedItem();
        configuration.setProperty("serialPort", serialPort.getSystemPortPath());
        configuration.setProperty("baudRate", baudRateComboBox.getSelectionModel().getSelectedItem().intValue());
    }

    @Override
    public boolean validate() {
        if (serialPortComboBox.getSelectionModel().getSelectedItem() == null) {
            return false;
        }
        if (baudRateComboBox.getSelectionModel().getSelectedItem() == null) {
            return false;
        }
        return true;
    }

}
