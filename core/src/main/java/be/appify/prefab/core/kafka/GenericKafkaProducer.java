package be.appify.prefab.core.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Generic Kafka producer that publishes any Spring application event whose type is registered
 * in the {@link EventRegistry} to the corresponding Kafka topic.
 */
@Component
@ConditionalOnClass(KafkaTemplate.class)
public class GenericKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(GenericKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventRegistry eventRegistry;

    public GenericKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, EventRegistry eventRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventRegistry = eventRegistry;
    }

    @EventListener
    public void publish(Object event) {
        try {
            var topic = eventRegistry.topicForType(event.getClass());
            log.debug("Publishing event {} on topic {}", event, topic);
            kafkaTemplate.send(topic, eventRegistry.keyFor(event).orElse(null), event).join();
        } catch (IllegalArgumentException e) {
            log.trace("Event type {} not registered in EventRegistry, skipping", event.getClass().getName());
        }
    }
}
