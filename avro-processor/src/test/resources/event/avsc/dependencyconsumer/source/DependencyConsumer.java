package event.avsc.dependencyconsumer;

import be.appify.prefab.core.annotations.EventHandler;
import event.avsc.dependency.DependencyAvsc;

public class DependencyConsumer {

    @EventHandler
    public void on(DependencyAvsc event) {
        // no-op
    }
}

