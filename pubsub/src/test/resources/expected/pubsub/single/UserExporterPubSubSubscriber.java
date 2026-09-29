package pubsub.single.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pubsub.single.UserCreated;
import pubsub.single.UserExporter;

@Component
public class UserExporterPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterPubSubSubscriber.class);

    private final UserExporter userExporter;

    public UserExporterPubSubSubscriber(UserExporter userExporter, PubSubUtil pubSub) {
        pubSub.subscribe(new SubscriptionRequest<UserCreated>("user", "user-exporter-on-user-created-user", UserCreated.class, this::onUserCreated)
                .withExecutor(Executors.newFixedThreadPool(2)));
        this.userExporter = userExporter;
    }

    private void onUserCreated(UserCreated event) {
        log.debug("Received event {}", event);
        userExporter.onUserCreated(event);
    }
}
