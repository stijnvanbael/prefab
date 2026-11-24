package kafka.avscnamespace;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class ShipmentProcessor {
    @EventHandler
    public void onShipmentDispatched(ShipmentEvent event) {
        // handle the event
    }
}
