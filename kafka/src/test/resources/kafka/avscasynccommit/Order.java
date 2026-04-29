package kafka.avscasynccommit;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) {
    @EventHandler
    public static Order onOrderPlaced(OrderPlacedEvent event) {
        return new Order(Reference.fromId(event.orderId()), event.customerId(), "PLACED");
    }
}

