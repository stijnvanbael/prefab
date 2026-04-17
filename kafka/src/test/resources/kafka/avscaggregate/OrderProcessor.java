package kafka.avscaggregate;
import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;
@Component
public class OrderProcessor {
    @EventHandler
    public void onOrderCreated(OrderCreatedEvent event) {
    }
    @EventHandler
    public void onOrderShipped(OrderShippedEvent event) {
    }
}
