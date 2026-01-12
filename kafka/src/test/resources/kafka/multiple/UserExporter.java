package kafka.multiple;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UserExporter {
    @EventHandler(concurrency = "${user-exporter.concurrency:4}")
    public void onUserCreated(UserEvent.Created event) {
        // handle the event
    }

    @EventHandler(concurrency = "${user-exporter.concurrency:4}")
    public void onUserUpdated(UserEvent.Updated event) {
        // handle the event
    }

    @EventHandler(concurrency = "${user-exporter.concurrency:4}")
    public void onUserDeleted(UserEvent.Deleted event) {
        // handle the event
    }
}
