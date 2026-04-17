package kafka.avscmulti.infrastructure.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.avscmulti.OrderEvent;
import kafka.avscmulti.OrderProcessor;
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
        typeResolver.registerType("prefab.order", OrderEvent.class);
        this.orderProcessor = orderProcessor;
    }

    @KafkaListener(
            topics = "prefab.order",
            groupId = "${spring.application.name}.order-processor-on-order-event",
            concurrency = "1"
    )
    public void onOrderEvent(OrderEvent event) {
        log.debug("Received event {}", event);
        orderProcessor.onOrderEvent(event);
    }
}
