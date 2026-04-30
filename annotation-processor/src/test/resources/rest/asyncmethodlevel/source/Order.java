package rest.asyncmethodlevel;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

@Aggregate
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) implements PublishesEvents {
    @Create
    @AsyncCommit
    public static OrderPlaced placeOrder(@NotNull String customerId) {
        return new OrderPlaced(Reference.create(), customerId);
    }

    @Update(path = "/cancel")
    @AsyncCommit
    public void cancel() {
        publish(new OrderCancelled(id));
    }

    @Event(topic = "orders")
    public record OrderPlaced(Reference<Order> id, String customerId) {
    }

    @Event(topic = "orders")
    public record OrderCancelled(Reference<Order> id) {
    }
}

