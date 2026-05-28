package pubsub.publishtoall.infrastructure.pubsub;

import be.appify.prefab.core.annotations.PublishTo;
import be.appify.prefab.core.pubsub.PubSubUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pubsub.publishtoall.UserEvent;

@Component
public class UserEventPubSubEventTypeRegistrar {
    public UserEventPubSubEventTypeRegistrar(PubSubUtil pubSubUtil,
            @Value("${topic.user.primary}") String userEventTopic0,
            @Value("${topic.user.secondary}") String userEventTopic1) {
        pubSubUtil.registerType(UserEvent.class.getName(), UserEvent.class);
        pubSubUtil.registerEventTopic(userEventTopic0, UserEvent.class);
        pubSubUtil.registerEventTopic(userEventTopic1, UserEvent.class);
        pubSubUtil.registerPublishTo(UserEvent.class, PublishTo.ALL);
    }
}

