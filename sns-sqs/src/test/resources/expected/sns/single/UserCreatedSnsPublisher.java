package sns.single.infrastructure.sns;

import be.appify.prefab.core.sns.SqsUtil;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sns.single.UserCreated;

@Component
public class UserCreatedSnsPublisher {
    private static final Logger log = LoggerFactory.getLogger(UserCreatedSnsPublisher.class);

    private final SnsTemplate snsTemplate;

    private final String topicArn;

    public UserCreatedSnsPublisher(SnsTemplate snsTemplate, SqsUtil sqsUtil) {
        this.snsTemplate = snsTemplate;
        this.topicArn = sqsUtil.ensureTopicExists("user");
    }

    @EventListener
    public void publish(UserCreated event) {
        log.debug("Publishing event {} on topic {}", event, topicArn);
        snsTemplate.sendNotification(topicArn, event, event.getClass().getName());
    }
}
