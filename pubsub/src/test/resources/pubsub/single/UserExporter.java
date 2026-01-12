package pubsub.single;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UserExporter {
    @EventHandler(concurrency = "2")
    public void onUserCreated(UserCreated event) {
        // handle the event
    }
}
