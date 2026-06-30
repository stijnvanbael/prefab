package kafka.avsc.infrastructure.event;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.EventRegistryCustomizer;
import kafka.avsc.OrderCreatedEvent;
import org.springframework.stereotype.Component;
@Component("kafka_avsc_OrderCreatedEventEventTypeRegistrar")
public class OrderCreatedEventEventTypeRegistrar implements EventRegistryCustomizer {
    @Override
    public void customize(EventRegistry registry) {
        registry.register("prefab.order", OrderCreatedEvent.class, Event.Serialization.AVRO);
    }
}

