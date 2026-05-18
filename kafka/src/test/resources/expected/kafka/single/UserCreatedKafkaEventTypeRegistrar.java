package kafka.single.infrastructure.kafka;
import be.appify.prefab.core.kafka.EventRegistry;
import kafka.single.UserCreated;
import org.springframework.stereotype.Component;
@Component
public class UserCreatedKafkaEventTypeRegistrar {
    public UserCreatedKafkaEventTypeRegistrar(EventRegistry eventRegistry) {
        eventRegistry.registerType("prefab.user", UserCreated.class, event -> event.user().id());
    }
}
