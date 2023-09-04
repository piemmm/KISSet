package org.prowl.kisset.eventbus.events;

public class ConfigurationChangeCompleteEvent extends BaseEvent {

    boolean interfacesWereChanged = false;
    public ConfigurationChangeCompleteEvent(boolean interfacesWereChanged) {
        this.interfacesWereChanged = interfacesWereChanged;
    }

    public boolean interfacesWereChanged() {
        return interfacesWereChanged;
    }

}
