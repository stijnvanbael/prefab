package pubsub.dltdisabled.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.dltdisabled.UserEvent;
import pubsub.dltdisabled.UserExporter;

@Component
public class UserExporterPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterPubSubSubscriber.class);

    private final Executor executor;

    private final UserExporter userExporter;

    public UserExporterPubSubSubscriber(UserExporter userExporter, PubSubUtil pubSub,
            @Value("${topic.user.name}") String userEventTopic) {
        executor = Executors.newFixedThreadPool(1);
        pubSub.subscribe(new SubscriptionRequest<UserEvent>(userEventTopic, "user-exporter-on-user-event", UserEvent.class, this::onUserEvent)
                .withExecutor(executor)
                .withDeadLetterPolicy(null));
        this.userExporter = userExporter;
    }

    private void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userExporter.onUserCreated(e);
            case UserEvent.Updated e -> userExporter.onUserUpdated(e);
            case UserEvent.Deleted e -> userExporter.onUserDeleted(e);
            default -> {
            }
        }
    }
}
