package pubsub.multiple;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UserEventHandler {
    @EventHandler
    public void onUserCreated(UserEvent.Created event) {
        // handle the event
    }

    @EventHandler
    public void onUserUpdated(UserEvent.Updated event) {
        // handle the event
    }

    @EventHandler
    public void onUserDeleted(UserEvent.Deleted event) {
        // handle the event
    }
}
