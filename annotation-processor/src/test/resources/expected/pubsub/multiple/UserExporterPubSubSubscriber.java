package pubsub.multiple.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.multiple.UserEvent;
import pubsub.multiple.UserExporter;

@Component
public class UserExporterPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterPubSubSubscriber.class);

    private final UserExporter userExporter;

    public UserExporterPubSubSubscriber(UserExporter userExporter, PubSubUtil pubSub,
            @Value("${topic.user.name}") String topic) {
        pubSub.subscribe(topic, "user-exporter-on-user-event", UserEvent.class, this::onUserEvent);
        this.userExporter = userExporter;
    }

    public void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userExporter.onUserCreated(e);
            case UserEvent.Updated e -> userExporter.onUserUpdated(e);
            case UserEvent.Deleted e -> userExporter.onUserDeleted(e);
            default -> {}
        }
    }
}
