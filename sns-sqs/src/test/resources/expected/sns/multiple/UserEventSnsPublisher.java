package sns.multiple.infrastructure.sns;

import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.sns.SqsUtil;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sns.multiple.UserEvent;

@Component
public class UserEventSnsPublisher {
    private static final Logger log = LoggerFactory.getLogger(UserEventSnsPublisher.class);

    private final SnsTemplate snsTemplate;

    private final SnsSerializer snsSerializer;

    private final String topic;

    private final String topicArn;

    public UserEventSnsPublisher(SnsTemplate snsTemplate, SqsUtil sqsUtil,
            SnsSerializer snsSerializer, @Value("${topic.user.name}") String topic) {
        this.snsTemplate = snsTemplate;
        this.snsSerializer = snsSerializer;
        this.topic = topic;
        this.topicArn = sqsUtil.ensureTopicExists(topic);
    }

    @EventListener
    public void publish(UserEvent event) {
        log.debug("Publishing event {} on topic {}", event, topicArn);
        CompletableFuture.runAsync(() -> snsTemplate.sendNotification(topicArn, snsSerializer.serialize(topic, event), event.getClass().getName())).join();
    }
}
