package pubsub.multitopic.infrastructure.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.multitopic.Refund;
import pubsub.multitopic.Sale;
import pubsub.multitopic.application.DayTotalService;

@Component
public class DayTotalPubSubSubscriber {
    private static final Logger log = LoggerFactory.getLogger(DayTotalPubSubSubscriber.class);

    private final Executor executor = Executors.newSingleThreadExecutor();

    private final DayTotalService dayTotalService;

    public DayTotalPubSubSubscriber(DayTotalService dayTotalService, PubSubUtil pubSub,
            @Value("${topic.sale.name}") String saleCreatedTopic,
            @Value("${topic.refund.name}") String refundCreatedTopic) {
        pubSub.subscribe(saleCreatedTopic, "day-total-on-sale-created", Sale.Created.class, this::onSaleCreated, executor);
        pubSub.subscribe(refundCreatedTopic, "day-total-on-refund-created", Refund.Created.class, this::onRefundCreated, executor);
        this.dayTotalService = dayTotalService;
    }

    private void onSaleCreated(Sale.Created event) {
        log.debug("Received event {}", event);
        dayTotalService.onSaleCreated(event);
    }

    private void onRefundCreated(Refund.Created event) {
        log.debug("Received event {}", event);
        dayTotalService.onRefundCreated(event);
    }
}
