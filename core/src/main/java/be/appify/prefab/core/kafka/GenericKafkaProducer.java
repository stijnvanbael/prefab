package be.appify.prefab.core.kafka;

import be.appify.prefab.core.domain.DomainEventDispatcher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Generic Kafka producer that dispatches domain events whose type is registered
 * in the {@link EventRegistry} directly to the corresponding Kafka topic.
 *
 * <p>Implements {@link DomainEventDispatcher} so that {@code SpringDomainEventPublisher}
 * can route {@code @Event}-annotated records here without going through the Spring
 * application-event bus.
 */
@Component
@ConditionalOnClass(KafkaTemplate.class)
public class GenericKafkaProducer implements DomainEventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(GenericKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventRegistry eventRegistry;

    public GenericKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, EventRegistry eventRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventRegistry = eventRegistry;
    }

    @Override
    public boolean canDispatch(Class<?> eventType) {
        return eventRegistry.hasTopicForType(eventType);
    }

    @Override
    public void dispatch(Object event) {
        publishToTopics(event, eventRegistry.topicsForDispatch(event));
    }

    /**
     * Dispatches {@code event} to the explicitly specified topics instead of the registered ones.
     * When no overrides are provided the call delegates to {@link #dispatch(Object)}.
     *
     * @param event          the domain event to dispatch
     * @param topicOverrides explicit target topics; empty means "use registry"
     */
    @Override
    public void dispatch(Object event, String... topicOverrides) {
        var topics = topicOverrides.length > 0 ? List.of(topicOverrides) : eventRegistry.topicsForDispatch(event);
        publishToTopics(event, topics);
    }

    private void publishToTopics(Object event, List<String> topics) {
        for (var topic : topics) {
            log.debug("Publishing event {} on topic {}", event, topic);
            kafkaTemplate.send(topic, eventRegistry.keyFor(event).orElse(null), event).join();
        }
    }
}
