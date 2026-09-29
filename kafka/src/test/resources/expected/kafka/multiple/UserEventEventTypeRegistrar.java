package kafka.multiple.infrastructure.event;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.multiple.UserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component("kafka_multiple_UserEventEventTypeRegistrar")
public class UserEventEventTypeRegistrar implements EventRegistryCustomizer {
    private final String[] userEventTopics;
    public UserEventEventTypeRegistrar(
            @Value("${topic.user.name}") String[] userEventTopics) {
        this.userEventTopics = userEventTopics;
    }
    @Override
    public void customize(EventRegistry registry) {
        for (var topic : userEventTopics) {
            registry.register(topic, UserEvent.class, Event.Serialization.JSON, event -> event.id());
        }
    }
}

