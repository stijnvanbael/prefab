package pubsub.publishtoall.infrastructure.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PublishTo;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.publishtoall.UserEvent;

@Component("pubsub_publishtoall_UserEventEventTypeRegistrar")
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
        registry.registerPublishTo(UserEvent.class, PublishTo.ALL);
    }
}

