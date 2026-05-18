package kafka.multitopic.infrastructure.kafka;
import be.appify.prefab.core.kafka.EventRegistry;
import kafka.multitopic.Sale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class SaleCreatedKafkaEventTypeRegistrar {
    public SaleCreatedKafkaEventTypeRegistrar(EventRegistry eventRegistry,
            @Value("${topic.sale.name}") String saleCreatedTopic) {
        eventRegistry.registerType(saleCreatedTopic, Sale.Created.class);
    }
}
