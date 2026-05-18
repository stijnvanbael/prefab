package kafka.supertype.infrastructure.kafka;

import be.appify.prefab.core.kafka.EventRegistry;
import kafka.supertype.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(UserEventKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final EventRegistry eventRegistry;

    private final String topic;

    public UserEventKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, EventRegistry eventRegistry,
            @Value("${topic.user.name}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventRegistry = eventRegistry;
        this.topic = topic;
    }

    @EventListener
    public void publish(UserEvent event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        kafkaTemplate.send(topic, eventRegistry.keyFor(event).orElse(null), event).join();
    }
}
