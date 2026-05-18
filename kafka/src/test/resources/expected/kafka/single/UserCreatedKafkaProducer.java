package kafka.single.infrastructure.kafka;

import be.appify.prefab.core.kafka.EventRegistry;
import kafka.single.UserCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(UserCreatedKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final EventRegistry eventRegistry;

    private final String topic;

    public UserCreatedKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, EventRegistry eventRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventRegistry = eventRegistry;
        this.topic = "prefab.user";
    }

    @EventListener
    public void publish(UserCreated event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        kafkaTemplate.send(topic, eventRegistry.keyFor(event).orElse(null), event).join();
    }
}
