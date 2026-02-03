package kafka.customdlt;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import org.springframework.stereotype.Component;

@Component
@EventHandlerConfig(
        deadLetterTopic = "${custom.dlt.name}",
        retryLimit = "10",
        minimumBackoffMs = "100",
        maximumBackoffMs = "10000"
)
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
