package kafka.multiple.infrastructure.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.multiple.UserEvent;
import kafka.multiple.UserEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserKafkaConsumer.class);

    private final UserEventHandler userEventHandler;

    public UserKafkaConsumer(UserEventHandler userEventHandler, KafkaJsonTypeResolver typeResolver,
            @Value("${topic.user.name}") String topic) {
        typeResolver.registerType(topic, UserEvent.class);
        this.userEventHandler = userEventHandler;
    }

    @KafkaListener(
            topics = "${topic.user.name}"
    )
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
