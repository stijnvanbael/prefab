package kafka.multiple.infrastructure.kafka;

import kafka.multiple.UserEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventKafkaProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final String topic;

    public UserEventKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("prefab.user") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @EventListener
    public void publish(UserEvent event) {
        kafkaTemplate.send(topic, event.id(), event);
    }
}
