package kafka.multiple.infrastructure.kafka;
import be.appify.prefab.core.kafka.EventRegistry;
import kafka.multiple.UserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class UserEventKafkaEventTypeRegistrar {
    public UserEventKafkaEventTypeRegistrar(EventRegistry eventRegistry,
            @Value("${topic.user.name}") String userEventTopic) {
        eventRegistry.registerType(userEventTopic, UserEvent.class, event -> event.id());
    }
}
