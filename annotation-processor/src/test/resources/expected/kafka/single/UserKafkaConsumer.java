package kafka.single.infrastructure.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.single.UserCreated;
import kafka.single.UserEventHandler;
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
            @Value("prefab.user") String topic) {
        this.userEventHandler = userEventHandler;
        typeResolver.registerType(topic, UserCreated.class);
    }

    @KafkaListener(
            topics = "prefab.user"
    )
    public void onUserCreated(UserCreated event) {
        log.debug("Received event {}", event);
        userEventHandler.onUserCreated(event);
    }
}
