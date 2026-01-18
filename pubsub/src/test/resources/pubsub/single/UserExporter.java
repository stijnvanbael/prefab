package pubsub.single;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import org.springframework.stereotype.Component;

@Component
@EventHandlerConfig(concurrency = "2")
public class UserExporter {
    @EventHandler
    public void onUserCreated(UserCreated event) {
        // handle the event
    }
}
