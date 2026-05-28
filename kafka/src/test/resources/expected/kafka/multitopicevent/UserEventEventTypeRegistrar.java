package kafka.multitopicevent.infrastructure.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.multitopicevent.UserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserEventEventTypeRegistrar implements EventRegistryCustomizer {
    private final String userEventTopic0;

    private final String userEventTopic1;

    public UserEventEventTypeRegistrar(
            @Value("${topic.user.primary}") String userEventTopic0,
            @Value("${topic.user.secondary}") String userEventTopic1) {
        this.userEventTopic0 = userEventTopic0;
        this.userEventTopic1 = userEventTopic1;
    }

    @Override
    public void customize(EventRegistry registry) {
        registry.register(userEventTopic0, UserEvent.class, Event.Serialization.JSON);
        registry.register(userEventTopic1, UserEvent.class, Event.Serialization.JSON);
    }
}

