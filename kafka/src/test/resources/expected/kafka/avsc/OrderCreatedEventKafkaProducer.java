package kafka.avsc.infrastructure.kafka;

import java.util.concurrent.CompletableFuture;
import kafka.avsc.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final String topic;

    public OrderCreatedEventKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = "prefab.order";
    }

    @EventListener
    public CompletableFuture<SendResult<String, Object>> publish(OrderCreatedEvent event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        return kafkaTemplate.send(topic, event);
    }
}
