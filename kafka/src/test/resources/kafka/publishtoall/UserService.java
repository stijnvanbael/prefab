package kafka.publishtoall;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UserService {
    @EventHandler
    public void on(UserEvent event) {
    }
}

