package kafka.single;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UserEventHandler {
    @EventHandler
    public void onUserCreated(UserCreated event) {
        // handle the event
    }
}
