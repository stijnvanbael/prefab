package rest.asyncupdate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
public record Order(
        @Id Reference<Order> id,
        String trackingCode,
        String status
) implements PublishesEvents {
    @Update(path = "/ship", method = "POST")
    public void ship(@NotNull String trackingCode) {
        publish(new OrderShipped(id, trackingCode));
    }

    @Event(topic = "orders")
    public record OrderShipped(Reference<Order> id, String trackingCode) {
    }
}

