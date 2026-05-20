package sns.multiple.infrastructure.sns;

import be.appify.prefab.core.sns.SqsUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sns.multiple.UserEvent;

@Component
public class UserEventSqsEventTypeRegistrar {
    public UserEventSqsEventTypeRegistrar(SqsUtil sqsUtil, @Value("${topic.user.name}") String userEventTopic) {
        sqsUtil.registerType(UserEvent.class.getName(), UserEvent.class);
        sqsUtil.registerEventTopic(userEventTopic, UserEvent.class);
    }
}

