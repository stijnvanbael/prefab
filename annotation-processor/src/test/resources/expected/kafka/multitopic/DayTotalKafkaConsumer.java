package kafka.multitopic.infrastructure.kafka;

import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.multitopic.Refund;
import kafka.multitopic.Sale;
import kafka.multitopic.application.DayTotalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DayTotalKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(DayTotalKafkaConsumer.class);

    private final DayTotalService dayTotalService;

    public DayTotalKafkaConsumer(DayTotalService dayTotalService,
            KafkaJsonTypeResolver typeResolver,
            @Value("${topic.sale.name}") String saleCreatedTopic,
            @Value("${topic.refund.name}") String refundCreatedTopic) {
        typeResolver.registerType(saleCreatedTopic, Sale.Created.class);
        typeResolver.registerType(refundCreatedTopic, Refund.Created.class);
        this.dayTotalService = dayTotalService;
    }

    @KafkaListener(
            topics = "${topic.sale.name}",
            groupId = "${spring.application.name}.day-total-on-sale-created"
    )
    public void onSaleCreated(Sale.Created event) {
        log.debug("Received event {}", event);
        dayTotalService.onSaleCreated(event);
    }

    @KafkaListener(
            topics = "${topic.refund.name}",
            groupId = "${spring.application.name}.day-total-on-refund-created"
    )
    public void onRefundCreated(Refund.Created event) {
        log.debug("Received event {}", event);
        dayTotalService.onRefundCreated(event);
    }
}
