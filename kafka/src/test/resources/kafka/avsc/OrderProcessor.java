package kafka.avsc;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class OrderProcessor {
    @EventHandler
    public void onOrderCreated(OrderCreated event) {
        // handle the event
    }
}