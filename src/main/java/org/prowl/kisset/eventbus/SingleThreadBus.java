package org.prowl.kisset.eventbus;

import com.google.common.eventbus.EventBus;
import org.prowl.kisset.eventbus.events.BaseEvent;

/**
 * Simple event bus
 */
public enum SingleThreadBus {

    INSTANCE;

    private EventBus eventBus = new EventBus();

    public final void post(final BaseEvent event) {
        eventBus.post(event);
    }

    public final void register(final Object o) {
        eventBus.register(o);
    }

    public final void unregister(final Object o) {
        eventBus.unregister(o);
    }

}