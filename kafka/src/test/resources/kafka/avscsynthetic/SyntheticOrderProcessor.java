package kafka.avscsynthetic;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class SyntheticOrderProcessor {
    @EventHandler
    public void onOrderCreated(SyntheticOrderEvents event) {
    }
}
