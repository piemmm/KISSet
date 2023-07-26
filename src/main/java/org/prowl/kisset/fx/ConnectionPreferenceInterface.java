package org.prowl.kisset.fx;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.prowl.kisset.config.Config;
import org.prowl.kisset.io.Interface;

public interface ConnectionPreferenceInterface {

    public void init(HierarchicalConfiguration configInterfaceNode, PreferencesController controller);

    public boolean validate();

    public void applyToConfig(HierarchicalConfiguration configuration);
}
