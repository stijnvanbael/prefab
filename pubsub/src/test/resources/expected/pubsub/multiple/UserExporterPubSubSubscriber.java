package pubsub.multiple.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.multiple.UserEvent;
import pubsub.multiple.UserExporter;

@Component
public class UserExporterPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterPubSubSubscriber.class);

    private final Executor executor = Executors.newSingleThreadExecutor();

    private final UserExporter userExporter;

    public UserExporterPubSubSubscriber(UserExporter userExporter, PubSubUtil pubSub,
            @Value("${topic.user.name}") String userEventTopic) {
        pubSub.subscribe(userEventTopic, "user-exporter-on-user-event", UserEvent.class, this::onUserEvent, executor);
        this.userExporter = userExporter;
    }

    private void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userExporter.onUserCreated(e);
            case UserEvent.Updated e -> userExporter.onUserUpdated(e);
            case UserEvent.Deleted e -> userExporter.onUserDeleted(e);
            default -> {}
        }
    }
}
