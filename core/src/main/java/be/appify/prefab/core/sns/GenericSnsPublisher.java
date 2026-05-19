package be.appify.prefab.core.sns;

import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Generic SNS publisher that publishes any Spring application event whose type is registered
 * in {@link SqsUtil} to the corresponding SNS topic.
 */
@Component
@ConditionalOnClass(SnsTemplate.class)
public class GenericSnsPublisher {
    private static final Logger log = LoggerFactory.getLogger(GenericSnsPublisher.class);

    private final SnsTemplate snsTemplate;
    private final SqsUtil sqsUtil;
    private final SnsSerializer snsSerializer;
    private final ConcurrentMap<String, String> topicArnCache = new ConcurrentHashMap<>();

    public GenericSnsPublisher(SnsTemplate snsTemplate, SqsUtil sqsUtil, SnsSerializer snsSerializer) {
        this.snsTemplate = snsTemplate;
        this.sqsUtil = sqsUtil;
        this.snsSerializer = snsSerializer;
    }

    @EventListener
    public void publish(Object event) {
        try {
            var topic = sqsUtil.topicForType(event.getClass());
            var topicArn = topicArnCache.computeIfAbsent(topic, sqsUtil::ensureTopicExists);
            log.debug("Publishing event {} on topic {}", event, topicArn);
            CompletableFuture.runAsync(() ->
                    snsTemplate.sendNotification(topicArn, snsSerializer.serialize(topic, event), event.getClass().getName())
            ).join();
        } catch (IllegalArgumentException e) {
            log.trace("Event type {} not registered in SqsUtil, skipping", event.getClass().getName());
        }
    }
}

