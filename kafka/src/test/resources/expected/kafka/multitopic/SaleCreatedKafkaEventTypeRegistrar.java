package kafka.multitopic.infrastructure.kafka;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.multitopic.Sale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class SaleCreatedKafkaEventTypeRegistrar implements EventRegistryCustomizer {
    private final String saleCreatedTopic;
    public SaleCreatedKafkaEventTypeRegistrar(
            @Value("${topic.sale.name}") String saleCreatedTopic) {
        this.saleCreatedTopic = saleCreatedTopic;
    }
    @Override
    public void customize(EventRegistry registry) {
        registry.register(saleCreatedTopic, Sale.Created.class, Event.Serialization.JSON);
    }
}
