package be.appify.prefab.core.sns;

import be.appify.prefab.core.domain.DomainEventDispatcher;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * Generic SNS publisher that dispatches domain events whose type is registered
 * in {@link SqsUtil} directly to the corresponding SNS topic.
 *
 * <p>Implements {@link DomainEventDispatcher} so that {@code SpringDomainEventPublisher}
 * can route {@code @Event}-annotated records here without going through the Spring
 * application-event bus.
 */
@Component
@ConditionalOnClass(SnsTemplate.class)
public class GenericSnsPublisher implements DomainEventDispatcher {
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

    @Override
    public boolean canDispatch(Class<?> eventType) {
        return sqsUtil.tryTopicForType(eventType).isPresent();
    }

    @Override
    public void dispatch(Object event, String... topicOverrides) {
        var topics = topicOverrides.length > 0 ? List.of(topicOverrides) : sqsUtil.topicsForDispatch(event);
        publishToTopics(event, topics);
    }

    private void publishToTopics(Object event, List<String> topics) {
        for (var resolvedTopic : topics) {
            var topicArn = topicArnCache.computeIfAbsent(resolvedTopic, sqsUtil::ensureTopicExists);
            log.debug("Publishing event {} on topic {}", event, topicArn);
            CompletableFuture.runAsync(() ->
                    snsTemplate.sendNotification(topicArn, snsSerializer.serialize(resolvedTopic, event),
                            event.getClass().getName())
            ).join();
        }
    }
}
