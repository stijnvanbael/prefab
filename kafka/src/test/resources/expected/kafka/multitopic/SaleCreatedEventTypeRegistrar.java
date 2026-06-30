package kafka.multitopic.infrastructure.event;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.multitopic.Sale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component("kafka_multitopic_SaleCreatedEventTypeRegistrar")
public class SaleCreatedEventTypeRegistrar implements EventRegistryCustomizer {
    private final String saleCreatedTopic;
    public SaleCreatedEventTypeRegistrar(
            @Value("${topic.sale.name}") String saleCreatedTopic) {
        this.saleCreatedTopic = saleCreatedTopic;
    }
    @Override
    public void customize(EventRegistry registry) {
        registry.register(saleCreatedTopic, Sale.Created.class, Event.Serialization.JSON);
    }
}

