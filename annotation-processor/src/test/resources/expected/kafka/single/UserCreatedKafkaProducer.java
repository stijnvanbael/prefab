package kafka.single.infrastructure.kafka;

import kafka.single.UserCreated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedKafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final String topic;

    public UserCreatedKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("prefab.user") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @EventListener
    public void publish(UserCreated event) {
        kafkaTemplate.send(topic, event.id(), event);
    }
}
