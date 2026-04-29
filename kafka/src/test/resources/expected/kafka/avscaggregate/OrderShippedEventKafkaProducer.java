package kafka.avscaggregate.infrastructure.kafka;

import java.util.concurrent.CompletableFuture;
import kafka.avscaggregate.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class OrderShippedEventKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderShippedEventKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final String topic;

    public OrderShippedEventKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = "prefab.order";
    }

    @EventListener
    public CompletableFuture<SendResult<String, Object>> publish(OrderShippedEvent event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        return kafkaTemplate.send(topic, event);
    }
}
