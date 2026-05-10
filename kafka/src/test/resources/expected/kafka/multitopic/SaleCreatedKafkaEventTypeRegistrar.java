package kafka.multitopic.infrastructure.kafka;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.multitopic.Sale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class SaleCreatedKafkaEventTypeRegistrar {
    public SaleCreatedKafkaEventTypeRegistrar(KafkaJsonTypeResolver typeResolver,
            @Value("${topic.sale.name}") String saleCreatedTopic) {
        typeResolver.registerType(saleCreatedTopic, Sale.Created.class);
    }
}
