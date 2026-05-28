package sns.multitopicevent.infrastructure.sns;

import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sns.multitopicevent.UserEvent;
import sns.multitopicevent.UserService;

@Component
public class UserServiceSqsSubscriber {
    private static final Logger log = LoggerFactory.getLogger(UserServiceSqsSubscriber.class);

    private final Executor executor;

    private final UserService userService;

    public UserServiceSqsSubscriber(UserService userService, SqsUtil sqsUtil,
            @Value("${topic.user.primary}") String userEvent0Topic,
            @Value("${topic.user.secondary}") String userEvent1Topic) {
        executor = Executors.newFixedThreadPool(1);
        sqsUtil.registerType(UserEvent.class.getName(), UserEvent.class);
        sqsUtil.subscribe(new SqsSubscriptionRequest<UserEvent>(userEvent0Topic, "user-service-on-user-event", UserEvent.class, this::onUserEvent)
                .withExecutor(executor));
        sqsUtil.registerType(UserEvent.class.getName(), UserEvent.class);
        sqsUtil.subscribe(new SqsSubscriptionRequest<UserEvent>(userEvent1Topic, "user-service-on-user-event", UserEvent.class, this::onUserEvent)
                .withExecutor(executor));
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

