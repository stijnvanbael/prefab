package event.handler.instancehandler;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderProjection {

    @EventHandler(Order.class)
    public void onOrderCreated(OrderCreated event) {
        // projection logic handled by this component
    }
}
