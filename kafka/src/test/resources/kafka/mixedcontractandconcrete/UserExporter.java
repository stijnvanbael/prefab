package kafka.mixedcontractandconcrete;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UserExporter {
    @EventHandler
    public void onAny(UserEvent event) {
        // no-op
    }

    @EventHandler
    public void onCreated(UserCreated event) {
        // no-op
    }
}

