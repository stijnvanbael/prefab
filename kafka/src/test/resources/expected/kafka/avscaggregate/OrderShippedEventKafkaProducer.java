package kafka.avscaggregate.infrastructure.kafka;

import be.appify.prefab.core.kafka.EventRegistry;
import kafka.avscaggregate.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderShippedEventKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderShippedEventKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final EventRegistry eventRegistry;

    private final String topic;

    public OrderShippedEventKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate, EventRegistry eventRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventRegistry = eventRegistry;
        this.topic = "prefab.order";
    }

    @EventListener
    public void publish(OrderShippedEvent event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        kafkaTemplate.send(topic, eventRegistry.keyFor(event).orElse(null), event).join();
    }
}
