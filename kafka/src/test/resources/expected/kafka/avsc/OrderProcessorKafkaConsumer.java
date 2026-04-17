package kafka.avsc.infrastructure.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.avsc.OrderCreatedEvent;
import kafka.avsc.OrderProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderProcessorKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessorKafkaConsumer.class);

    private final OrderProcessor orderProcessor;

    public OrderProcessorKafkaConsumer(OrderProcessor orderProcessor,
            KafkaJsonTypeResolver typeResolver) {
        typeResolver.registerType("prefab.order", OrderCreatedEvent.class);
        this.orderProcessor = orderProcessor;
    }

    @KafkaListener(
            topics = "prefab.order",
            groupId = "${spring.application.name}.order-processor-on-order-created",
            concurrency = "1"
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        log.debug("Received event {}", event);
        orderProcessor.onOrderCreated(event);
    }
}
