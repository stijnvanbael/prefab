package kafka.multiple;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import org.springframework.stereotype.Component;

@Component
@EventHandlerConfig(concurrency = "${user-exporter.concurrency:4}")
public class UserExporter {
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
