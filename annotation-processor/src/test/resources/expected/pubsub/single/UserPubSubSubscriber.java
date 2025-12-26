package pubsub.single.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.single.UserCreated;
import pubsub.single.UserEventHandler;

@Component
public class UserPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserPubSubSubscriber.class);

    private final UserEventHandler userEventHandler;

    public UserPubSubSubscriber(UserEventHandler userEventHandler, PubSubUtil pubSub,
            @Value("user") String topic) {
        this.userEventHandler = userEventHandler;
        pubSub.subscribe(topic, "user-on-user-created", UserCreated.class, this::onUserCreated);
    }

    public void onUserCreated(UserCreated event) {
        log.debug("Received event {}", event);
        userEventHandler.onUserCreated(event);
    }
}
