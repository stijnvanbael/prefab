package kafka.consumefromtopics;

import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import org.springframework.stereotype.Component;

@Component
@EventHandlerConfig(consumeFromTopics = "${topic.user.primary}")
public class UserService {

    @EventHandler
    public void onUserCreated(UserEvent.Created event) {
    }

    @EventHandler
    public void onUserUpdated(UserEvent.Updated event) {
    }
}

