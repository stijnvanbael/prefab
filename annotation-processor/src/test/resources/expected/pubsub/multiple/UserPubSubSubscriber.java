package pubsub.multiple.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.multiple.UserEvent;
import pubsub.multiple.UserEventHandler;

@Component
public class UserPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserPubSubSubscriber.class);

    private final UserEventHandler userEventHandler;

    public UserPubSubSubscriber(UserEventHandler userEventHandler, PubSubUtil pubSub,
            @Value("${topic.user.name}") String topic) {
        pubSub.subscribe(topic, "user-on-user-event", UserEvent.class, this::onUserEvent);
        this.userEventHandler = userEventHandler;
    }

    public void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userEventHandler.onUserCreated(e);
            case UserEvent.Updated e -> userEventHandler.onUserUpdated(e);
            case UserEvent.Deleted e -> userEventHandler.onUserDeleted(e);
            default -> {}
        }
    }
}
