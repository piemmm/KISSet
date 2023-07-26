package org.prowl.kisset.fx;

import org.apache.commons.configuration.HierarchicalConfiguration;

public class SerialPortConnectionPreference implements ConnectionPreferenceInterface {


    private PreferencesController preferencesController;
    private HierarchicalConfiguration configInterfaceNode;


    @Override
    public void init(HierarchicalConfiguration configInterfaceNode, PreferencesController preferencesController) {
        this.preferencesController = preferencesController;
        this.configInterfaceNode = configInterfaceNode;
    }

    @Override
    public void applyToConfig(HierarchicalConfiguration config) {

    }

    @Override
    public boolean validate() {
        return false;
    }

}
