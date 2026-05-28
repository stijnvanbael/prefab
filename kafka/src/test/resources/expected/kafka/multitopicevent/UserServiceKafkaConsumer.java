package kafka.multitopicevent.infrastructure.kafka;

import kafka.multitopicevent.UserEvent;
import kafka.multitopicevent.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserServiceKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserServiceKafkaConsumer.class);

    private final UserService userService;

    public UserServiceKafkaConsumer(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(
            topics = {"${topic.user.primary}", "${topic.user.secondary}"},
            groupId = "${spring.application.name}.user-service-on-user-event",
            concurrency = "1"
    )
    public void onUserEvent(UserEvent event) {
        log.debug("Received event {}", event);
        switch (event) {
            case UserEvent.Created e -> userService.onUserCreated(e);
            case UserEvent.Updated e -> userService.onUserUpdated(e);
            default -> {
            }
        }
    }
}

