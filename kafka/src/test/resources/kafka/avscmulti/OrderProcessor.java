package kafka.avscmulti;
import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;
@Component
public class OrderProcessor {
    @EventHandler
    public void onOrderEvent(OrderEvent event) {
        // handle the event
    }
}
