package pubsub.multitopicevent.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import java.util.concurrent.Executor;
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

    private final Executor userEvent0Executor;

    private final Executor userEvent1Executor;

    private final UserService userService;

    public UserServicePubSubSubscriber(UserService userService, PubSubUtil pubSub,
            @Value("${topic.user.primary}") String userEvent0Topic,
            @Value("${topic.user.secondary}") String userEvent1Topic) {
        userEvent0Executor = Executors.newFixedThreadPool(1);
        pubSub.registerType(UserEvent.class.getName(), UserEvent.class);
        pubSub.subscribe(new SubscriptionRequest<UserEvent>(userEvent0Topic, "user-service-on-user-event", UserEvent.class, this::onUserEvent)
                .withExecutor(userEvent0Executor));
        userEvent1Executor = Executors.newFixedThreadPool(1);
        pubSub.registerType(UserEvent.class.getName(), UserEvent.class);
        pubSub.subscribe(new SubscriptionRequest<UserEvent>(userEvent1Topic, "user-service-on-user-event", UserEvent.class, this::onUserEvent)
                .withExecutor(userEvent1Executor));
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

