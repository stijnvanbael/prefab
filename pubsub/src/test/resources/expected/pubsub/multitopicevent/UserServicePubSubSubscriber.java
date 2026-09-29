package pubsub.multitopicevent.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.multitopicevent.UserEvent;
import pubsub.multitopicevent.UserService;

@Component
public class UserServicePubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserServicePubSubSubscriber.class);

    private final UserService userService;

    public UserServicePubSubSubscriber(UserService userService, PubSubUtil pubSub,
            @Value("${topic.user.primary}") String[] userEvent0Topics,
            @Value("${topic.user.secondary}") String[] userEvent1Topics) {
        for (var topic : userEvent0Topics) {
            pubSub.subscribe(new SubscriptionRequest<UserEvent>(topic, "user-service-on-user-event-" + topic, UserEvent.class, this::onUserEvent)
                    .withExecutor(Executors.newFixedThreadPool(1)));
        }
        for (var topic : userEvent1Topics) {
            pubSub.subscribe(new SubscriptionRequest<UserEvent>(topic, "user-service-on-user-event-" + topic, UserEvent.class, this::onUserEvent)
                    .withExecutor(Executors.newFixedThreadPool(1)));
        }
        this.userService = userService;
    }

    private void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userService.onUserCreated(e);
            case UserEvent.Updated e -> userService.onUserUpdated(e);
            default -> {
            }
        }
    }
}
