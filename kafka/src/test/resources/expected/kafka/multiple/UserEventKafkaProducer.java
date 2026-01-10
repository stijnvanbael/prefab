package kafka.multiple.infrastructure.kafka;

import kafka.multiple.UserEvent;
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

    private final String topic;

    public UserEventKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${topic.user.name}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @EventListener
    public void publish(UserEvent event) {
        log.debug("Publishing event {} on topic {}", event, topic);
        kafkaTemplate.send(topic, event.id(), event);
    }
}
