package kafka.createorupdate.infrastructure.event;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.createorupdate.MessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component("kafka_createorupdate_MessageEventEventTypeRegistrar")
public class MessageEventEventTypeRegistrar implements EventRegistryCustomizer {
    private final String messageEventTopic;
    public MessageEventEventTypeRegistrar(
            @Value("${topic.message.name}") String messageEventTopic) {
        this.messageEventTopic = messageEventTopic;
    }
    @Override
    public void customize(EventRegistry registry) {
        registry.register(messageEventTopic, MessageEvent.class, Event.Serialization.JSON);
    }
}

