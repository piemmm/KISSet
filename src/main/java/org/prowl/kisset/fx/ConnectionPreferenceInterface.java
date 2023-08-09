package org.prowl.kisset.fx;

import org.apache.commons.configuration.HierarchicalConfiguration;

public interface ConnectionPreferenceInterface {

    void init(HierarchicalConfiguration configInterfaceNode, PreferencesController controller, ConnectionPreferenceHost host);

    boolean validate();

    void applyToConfig(HierarchicalConfiguration configuration);
}
