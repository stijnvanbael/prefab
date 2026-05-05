package mother.dependencyconsumer.source;

import be.appify.prefab.core.annotations.EventHandler;
import mother.dependency.source.DependencyEvent;

public class DependencyConsumer {

    @EventHandler
    public void on(DependencyEvent event) {
        // no-op
    }
}

