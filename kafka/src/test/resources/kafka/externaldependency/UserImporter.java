package kafka.externaldependency;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import kafka.dependencyevents.ExternalUserCreated;
import org.springframework.stereotype.Component;

@Component
@EventHandlerConfig(concurrency = "2")
public class UserImporter {
    @EventHandler
    public void onExternalUserCreated(ExternalUserCreated event) {
        // handle the event
    }
}

