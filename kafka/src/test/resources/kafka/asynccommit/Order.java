package kafka.asynccommit;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
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
    public static Order onOrderPlaced(OrderPlaced event) {
        return new Order(event.id(), event.customerId(), "PLACED");
    }

    @Event(topic = "orders", platform = Event.Platform.KAFKA)
    public record OrderPlaced(Reference<Order> id, String customerId) {
    }
}

