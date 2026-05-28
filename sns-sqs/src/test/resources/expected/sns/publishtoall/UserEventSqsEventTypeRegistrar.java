package sns.publishtoall.infrastructure.sns;

import be.appify.prefab.core.annotations.PublishTo;
import be.appify.prefab.core.sns.SqsUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sns.publishtoall.UserEvent;

@Component
public class UserEventSqsEventTypeRegistrar {
    public UserEventSqsEventTypeRegistrar(SqsUtil sqsUtil,
            @Value("${topic.user.primary}") String userEventTopic0,
            @Value("${topic.user.secondary}") String userEventTopic1) {
        sqsUtil.registerType(UserEvent.class.getName(), UserEvent.class);
        sqsUtil.registerEventTopic(userEventTopic0, UserEvent.class);
        sqsUtil.registerEventTopic(userEventTopic1, UserEvent.class);
        sqsUtil.registerPublishTo(UserEvent.class, PublishTo.ALL);
    }
}

