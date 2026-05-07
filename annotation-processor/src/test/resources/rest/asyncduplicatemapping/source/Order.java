package rest.asyncduplicatemapping;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) {
    @Create
    public static void placeOrder(@NotNull String customerId) {
        PublishesEvents.publishEvent(new OrderPlaced(Reference.create(), customerId));
    }

    @Create
    public static void quickOrder(@NotNull String customerId) {
        PublishesEvents.publishEvent(new OrderPlaced(Reference.create(), customerId));
    }

    @Event(topic = "orders")
    public record OrderPlaced(Reference<Order> id, String customerId) {
    }
}
