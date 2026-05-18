package kafka.multitopic.infrastructure.kafka;
import be.appify.prefab.core.kafka.EventRegistry;
import kafka.multitopic.Refund;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class RefundCreatedKafkaEventTypeRegistrar {
    public RefundCreatedKafkaEventTypeRegistrar(EventRegistry eventRegistry,
            @Value("${topic.refund.name}") String refundCreatedTopic) {
        eventRegistry.registerType(refundCreatedTopic, Refund.Created.class);
    }
}
