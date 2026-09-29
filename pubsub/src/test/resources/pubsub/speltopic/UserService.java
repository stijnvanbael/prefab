package pubsub.speltopic;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import org.springframework.stereotype.Component;

@Component
@EventHandlerConfig(deadLetterTopic = "#{'${topic.prefix}' + '.dlt'}")
public class UserService {

    @EventHandler
    public void onUserCreated(UserEvent.Created event) {
    }
}
