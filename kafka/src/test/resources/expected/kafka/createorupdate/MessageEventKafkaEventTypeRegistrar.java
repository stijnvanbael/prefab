package kafka.createorupdate.infrastructure.kafka;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.createorupdate.MessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class MessageEventKafkaEventTypeRegistrar implements EventRegistryCustomizer {
    private final String messageEventTopic;
    public MessageEventKafkaEventTypeRegistrar(
            @Value("${topic.message.name}") String messageEventTopic) {
        this.messageEventTopic = messageEventTopic;
    }
    @Override
    public void customize(EventRegistry registry) {
        registry.register(messageEventTopic, MessageEvent.class, Event.Serialization.JSON);
    }
}
