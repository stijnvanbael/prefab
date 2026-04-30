package sns.multitopic.infrastructure.sns;

import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sns.multitopic.Refund;
import sns.multitopic.Sale;
import sns.multitopic.application.DayTotalService;

@Component
public class DayTotalSqsSubscriber {
    private static final Logger log = LoggerFactory.getLogger(DayTotalSqsSubscriber.class);

    private final Executor executor;

    private final DayTotalService dayTotalService;

    public DayTotalSqsSubscriber(DayTotalService dayTotalService, SqsUtil sqsUtil,
            @Value("${topic.sale.name}") String saleCreatedTopic,
            @Value("${topic.refund.name}") String refundCreatedTopic) {
        executor = Executors.newFixedThreadPool(1);
        sqsUtil.registerType(Sale.Created.class.getName(), Sale.Created.class);
        sqsUtil.subscribe(new SqsSubscriptionRequest<Sale.Created>(saleCreatedTopic, "day-total-on-sale-created", Sale.Created.class, this::onSaleCreated)
                .withExecutor(executor));
        sqsUtil.registerType(Refund.Created.class.getName(), Refund.Created.class);
        sqsUtil.subscribe(new SqsSubscriptionRequest<Refund.Created>(refundCreatedTopic, "day-total-on-refund-created", Refund.Created.class, this::onRefundCreated)
                .withExecutor(executor));
        this.dayTotalService = dayTotalService;
    }

    private void onRefundCreated(Refund.Created event) {
        log.debug("Received event {}", event);
        dayTotalService.onRefundCreated(event);
    }

    private void onSaleCreated(Sale.Created event) {
        log.debug("Received event {}", event);
        dayTotalService.onSaleCreated(event);
    }
}
