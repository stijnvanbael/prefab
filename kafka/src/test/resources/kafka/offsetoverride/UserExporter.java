package kafka.offsetoverride;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import org.springframework.stereotype.Component;

@Component
@EventHandlerConfig(autoOffsetReset = "${offset.override:latest}")
public class UserExporter {
    @EventHandler
    public void onUserCreated(UserCreated event) {
        // handle the event
    }
}

