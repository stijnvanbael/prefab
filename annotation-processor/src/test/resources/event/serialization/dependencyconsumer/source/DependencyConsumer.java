package event.serialization.dependencyconsumer;

import be.appify.prefab.core.annotations.EventHandler;
import event.serialization.dependency.DependencyEvent;

public class DependencyConsumer {

    @EventHandler
    public void on(DependencyEvent event) {
        // no-op
    }
}

