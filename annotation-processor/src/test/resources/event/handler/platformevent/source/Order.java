package event.handler.platformevent;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
public record Order(
        @Id Reference<Order> id
) {
    @EventHandler
    public static Order onCreate(OrderCreated event) {
        return new Order(Reference.create());
    }
}
