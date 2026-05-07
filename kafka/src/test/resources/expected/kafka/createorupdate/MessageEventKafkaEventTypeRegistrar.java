package kafka.createorupdate.infrastructure.kafka;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.createorupdate.MessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class MessageEventKafkaEventTypeRegistrar {
    public MessageEventKafkaEventTypeRegistrar(KafkaJsonTypeResolver typeResolver,
            @Value("${topic.message.name}") String messageEventTopic) {
        typeResolver.registerType(messageEventTopic, MessageEvent.class);
    }
}
