package sns.single.infrastructure.sns;

import be.appify.prefab.core.sns.SqsUtil;
import org.springframework.stereotype.Component;
import sns.single.UserCreated;

@Component
public class UserCreatedSqsEventTypeRegistrar {
    public UserCreatedSqsEventTypeRegistrar(SqsUtil sqsUtil) {
        sqsUtil.registerType(UserCreated.class.getName(), UserCreated.class);
        sqsUtil.registerEventTopic("user", UserCreated.class);
    }
}

