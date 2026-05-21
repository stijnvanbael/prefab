package kafka.single.infrastructure.kafka;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.single.UserCreated;
import org.springframework.stereotype.Component;
@Component
public class UserCreatedKafkaEventTypeRegistrar implements EventRegistryCustomizer {
    @Override
    public void customize(EventRegistry registry) {
        registry.register("prefab.user", UserCreated.class, Event.Serialization.JSON, event -> event.user().id());
    }
}
