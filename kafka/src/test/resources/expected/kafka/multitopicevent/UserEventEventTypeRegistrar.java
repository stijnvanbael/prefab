package kafka.multitopicevent.infrastructure.event;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.multitopicevent.UserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("kafka_multitopicevent_UserEventEventTypeRegistrar")
public class UserEventEventTypeRegistrar implements EventRegistryCustomizer {
    private final String[] userEventTopics0;

    private final String[] userEventTopics1;

    public UserEventEventTypeRegistrar(
            @Value("${topic.user.primary}") String[] userEventTopics0,
            @Value("${topic.user.secondary}") String[] userEventTopics1) {
        this.userEventTopics0 = userEventTopics0;
        this.userEventTopics1 = userEventTopics1;
    }

    @Override
    public void customize(EventRegistry registry) {
        for (var topic : userEventTopics0) {
            registry.register(topic, UserEvent.class, Event.Serialization.JSON);
        }
        for (var topic : userEventTopics1) {
            registry.register(topic, UserEvent.class, Event.Serialization.JSON);
        }
    }
}

