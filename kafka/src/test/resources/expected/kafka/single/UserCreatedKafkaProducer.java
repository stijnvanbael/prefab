package kafka.single.infrastructure.kafka;

import java.util.concurrent.CompletableFuture;
import kafka.single.UserCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(UserCreatedKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final String topic;

    public UserCreatedKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = "prefab.user";
    }

    @EventListener
    public CompletableFuture<SendResult<String, Object>> publish(UserCreated event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        return kafkaTemplate.send(topic, event.user().id(), event);
    }
}
