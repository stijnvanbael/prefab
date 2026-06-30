package pubsub.single.infrastructure.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import org.springframework.stereotype.Component;
import pubsub.single.UserCreated;

@Component("pubsub_single_UserCreatedEventTypeRegistrar")
public class UserCreatedEventTypeRegistrar implements EventRegistryCustomizer {
    @Override
    public void customize(EventRegistry registry) {
        registry.register("user", UserCreated.class, Event.Serialization.JSON, event -> event.user().id());
    }
}

