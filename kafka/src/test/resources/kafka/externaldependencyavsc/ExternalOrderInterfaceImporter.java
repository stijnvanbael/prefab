package kafka.externaldependencyavsc;

import be.appify.prefab.core.annotations.EventHandler;
import kafka.dependencyavsc.ExternalOrderCreated;
import org.springframework.stereotype.Component;

@Component
public class ExternalOrderInterfaceImporter {
    @EventHandler
    public void onExternalOrderCreated(ExternalOrderCreated event) {
        // handle the event
    }
}

