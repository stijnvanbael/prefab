package kafka.externaldependencyavsc;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import kafka.dependencyavsc.ExternalOrderCreatedEvent;
import org.springframework.stereotype.Component;

@Component
@EventHandlerConfig(concurrency = "2")
public class ExternalOrderImporter {
    @EventHandler
    public void onExternalOrderCreated(ExternalOrderCreatedEvent event) {
        // handle the event
    }
}

