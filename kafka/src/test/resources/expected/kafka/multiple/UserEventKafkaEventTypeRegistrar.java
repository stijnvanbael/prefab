package kafka.multiple.infrastructure.kafka;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.multiple.UserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class UserEventKafkaEventTypeRegistrar {
    public UserEventKafkaEventTypeRegistrar(KafkaJsonTypeResolver typeResolver,
            @Value("${topic.user.name}") String userEventTopic) {
        typeResolver.registerType(userEventTopic, UserEvent.class);
    }
}
