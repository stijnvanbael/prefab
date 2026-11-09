package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.DomainEventDispatcher;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.serialization.ConcreteEventSupport;
import be.appify.prefab.core.versioning.EventVersions;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
        return sqsUtil.hasTopicForType(eventType);
    }

    @Override
    public boolean canDispatch(EventType<?> eventType) {
        return sqsUtil.hasTopicForType(eventType)
                && ConcreteEventSupport.usesPlatform(eventType, Event.Platform.SNS_SQS);
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
            var payload = snsSerializer.serialize(resolvedTopic, event);
            var versionHeaders = new HashMap<>(EventVersions.headersFor(event));
            if (event instanceof TypedEvent<?> typed) {
                versionHeaders.put(EventType.HEADER, typed.type().id());
            }
            var subject = TypedEvent.payload(event).getClass().getName();
            CompletableFuture.runAsync(() -> {
                if (versionHeaders.isEmpty()) {
                    snsTemplate.sendNotification(topicArn, payload, subject);
                } else {
                    snsTemplate.sendNotification(topicArn, SnsNotification.builder(payload)
                            .subject(subject)
                            .headers(Map.copyOf(versionHeaders))
                            .build());
                }
            }).join();
        }
    }
}
