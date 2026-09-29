package kafka.multitopic.infrastructure.event;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.multitopic.Refund;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component("kafka_multitopic_RefundCreatedEventTypeRegistrar")
public class RefundCreatedEventTypeRegistrar implements EventRegistryCustomizer {
    private final String[] refundCreatedTopics;
    public RefundCreatedEventTypeRegistrar(
            @Value("${topic.refund.name}") String[] refundCreatedTopics) {
        this.refundCreatedTopics = refundCreatedTopics;
    }
    @Override
    public void customize(EventRegistry registry) {
        for (var topic : refundCreatedTopics) {
            registry.register(topic, Refund.Created.class, Event.Serialization.JSON);
        }
    }
}

