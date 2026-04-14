package event.handler.mergedhandler;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
public record OrderSummary(
        @Id Reference<OrderSummary> id,
        String orderId
) {
    @EventHandler(Order.class)
    public static OrderSummary onOrderCreated(OrderCreated event) {
        return new OrderSummary(Reference.create(), event.orderId());
    }
}
