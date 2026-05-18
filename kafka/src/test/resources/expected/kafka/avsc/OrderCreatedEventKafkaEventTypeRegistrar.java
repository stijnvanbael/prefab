package kafka.avsc.infrastructure.kafka;
import be.appify.prefab.core.kafka.EventRegistry;
import kafka.avsc.OrderCreatedEvent;
import org.springframework.stereotype.Component;
@Component
public class OrderCreatedEventKafkaEventTypeRegistrar {
    public OrderCreatedEventKafkaEventTypeRegistrar(EventRegistry eventRegistry) {
        eventRegistry.registerType("prefab.order", OrderCreatedEvent.class);
    }
}
