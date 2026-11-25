package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.DomainEventDispatcher;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.serialization.ConcreteEventSupport;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.pubsub.v1.PubsubMessage;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * Generic Pub/Sub publisher that dispatches domain events whose type is registered
 * in {@link PubSubUtil} directly to the corresponding Pub/Sub topic.
 *
 * <p>Implements {@link DomainEventDispatcher} so that {@code SpringDomainEventPublisher}
 * can route {@code @Event}-annotated records here without going through the Spring
 * application-event bus.
 */
@Component
@ConditionalOnClass(PubSubAdmin.class)
public class GenericPubSubPublisher implements DomainEventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(GenericPubSubPublisher.class);

    private final PubSubTemplate pubSubTemplate;
    private final PubSubUtil pubSubUtil;
    private final PubSubMessageEncoder encoder;
    private final ConcurrentMap<String, String> fullyQualifiedTopicCache = new ConcurrentHashMap<>();

    public GenericPubSubPublisher(PubSubTemplate pubSubTemplate, PubSubUtil pubSubUtil, PubSubSerializer serializer) {
        this.pubSubTemplate = pubSubTemplate;
        this.pubSubUtil = pubSubUtil;
        this.encoder = new PubSubMessageEncoder(serializer, event -> pubSubUtil.keyFor(event));
    }

    @Override
    public boolean canDispatch(Class<?> eventType) {
        return pubSubUtil.hasTopicForType(eventType);
    }

    @Override
    public boolean canDispatch(EventType<?> eventType) {
        return pubSubUtil.hasTopicForType(eventType)
                && ConcreteEventSupport.usesPlatform(eventType, Event.Platform.PUB_SUB);
    }

    @Override
    public void dispatch(Object event, String... topicOverrides) {
        var topics = topicOverrides.length > 0 ? List.of(topicOverrides) : pubSubUtil.topicsForDispatch(event);
        publishToTopics(event, topics);
    }

    private void publishToTopics(Object event, List<String> topics) {
        for (var resolvedTopic : topics) {
            var qualifiedTopic = fullyQualifiedTopicCache.computeIfAbsent(resolvedTopic, pubSubUtil::ensureTopicExists);
            log.debug("Publishing event {} on topic {}", event, qualifiedTopic);
            var message = encoder.encode(PubSubUtil.simpleTopicName(qualifiedTopic), event);
            publish(qualifiedTopic, message);
        }
    }

    private void publish(String qualifiedTopic, PubsubMessage message) {
        try {
            pubSubTemplate.publish(qualifiedTopic, message).join();
        } catch (CompletionException e) {
            throw new CompletionException(
                    "Failed to publish to Pub/Sub topic [%s], make sure Pub/Sub is available at the specified endpoint"
                            .formatted(qualifiedTopic), e.getCause() != null ? e.getCause() : e);
        }
    }
}
