package pubsub.single.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pubsub.single.UserCreated;
import pubsub.single.UserExporter;

@Component
public class UserExporterPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserExporterPubSubSubscriber.class);

    private final Executor userCreatedExecutor;

    private final UserExporter userExporter;

    public UserExporterPubSubSubscriber(UserExporter userExporter, PubSubUtil pubSub) {
        userCreatedExecutor = Executors.newFixedThreadPool(2);
        pubSub.subscribe(new SubscriptionRequest<UserCreated>("user", "user-exporter-on-user-created", UserCreated.class, this::onUserCreated)
                .withExecutor(userCreatedExecutor));
        this.userExporter = userExporter;
    }

    private void onUserCreated(UserCreated event) {
        log.debug("Received event {}", event);
        userExporter.onUserCreated(event);
    }
}
