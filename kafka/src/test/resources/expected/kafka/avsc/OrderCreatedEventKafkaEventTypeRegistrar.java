package kafka.avsc.infrastructure.kafka;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.avsc.OrderCreatedEvent;
import org.springframework.stereotype.Component;
@Component
public class OrderCreatedEventKafkaEventTypeRegistrar {
    public OrderCreatedEventKafkaEventTypeRegistrar(KafkaJsonTypeResolver typeResolver) {
        typeResolver.registerType("prefab.order", OrderCreatedEvent.class);
    }
}
