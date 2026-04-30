package rest.asyncduplicatemapping;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Create;
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
    public static OrderPlaced placeOrder(@NotNull String customerId) {
        return new OrderPlaced(Reference.create(), customerId);
    }

    @Create
    public static OrderPlaced quickOrder(@NotNull String customerId) {
        return new OrderPlaced(Reference.create(), customerId);
    }

    @Event(topic = "orders")
    public record OrderPlaced(Reference<Order> id, String customerId) {
    }
}
