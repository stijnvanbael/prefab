package kafka.createorupdate.infrastructure.kafka;
import be.appify.prefab.core.kafka.EventRegistry;
import kafka.createorupdate.MessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class MessageEventKafkaEventTypeRegistrar {
    public MessageEventKafkaEventTypeRegistrar(EventRegistry eventRegistry,
            @Value("${topic.message.name}") String messageEventTopic) {
        eventRegistry.registerType(messageEventTopic, MessageEvent.class);
    }
}
