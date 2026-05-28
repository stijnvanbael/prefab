package kafka.multitopicevent;

import be.appify.prefab.core.annotations.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class UserService {

    @EventHandler
    public void onUserCreated(UserEvent.Created event) {
    }

    @EventHandler
    public void onUserUpdated(UserEvent.Updated event) {
    }
}
