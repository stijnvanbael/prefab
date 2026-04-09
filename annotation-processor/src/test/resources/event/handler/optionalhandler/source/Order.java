package event.handler.optionalhandler;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import java.util.Optional;
import org.springframework.data.annotation.Id;

@Aggregate
public record Order(
        @Id Reference<Order> id
) {
    @EventHandler
    public static Optional<Order> onCreate(OrderCreated event) {
        return Optional.of(new Order(Reference.create()));
    }
}
