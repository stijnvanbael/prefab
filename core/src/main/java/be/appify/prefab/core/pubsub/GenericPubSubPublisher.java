package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.domain.DomainEventDispatcher;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
    private final PubSubSerializer serializer;
    private final ConcurrentMap<String, String> fullyQualifiedTopicCache = new ConcurrentHashMap<>();

    public GenericPubSubPublisher(PubSubTemplate pubSubTemplate, PubSubUtil pubSubUtil, PubSubSerializer serializer) {
        this.pubSubTemplate = pubSubTemplate;
        this.pubSubUtil = pubSubUtil;
        this.serializer = serializer;
    }

    @Override
    public boolean canDispatch(Class<?> eventType) {
        return pubSubUtil.tryTopicForType(eventType).isPresent();
    }

    @Override
    public void dispatch(Object event) {
        publishToTopics(event, pubSubUtil.topicsForDispatch(event));
    }

    /**
     * Dispatches {@code event} to the explicitly specified topics instead of the registered ones.
     * When no overrides are provided the call delegates to {@link #dispatch(Object)}.
     *
     * @param event          the domain event to dispatch
     * @param topicOverrides explicit target topics; empty means "use registry"
     */
    @Override
    public void dispatch(Object event, String... topicOverrides) {
        var topics = topicOverrides.length > 0 ? List.of(topicOverrides) : pubSubUtil.topicsForDispatch(event);
        publishToTopics(event, topics);
    }

    private void publishToTopics(Object event, List<String> topics) {
        for (var resolvedTopic : topics) {
            var qualifiedTopic = fullyQualifiedTopicCache.computeIfAbsent(resolvedTopic, pubSubUtil::ensureTopicExists);
            log.debug("Publishing event {} on topic {}", event, qualifiedTopic);
            var data = ByteString.copyFrom(serializer.serialize(PubSubUtil.simpleTopicName(qualifiedTopic), event));
            var messageBuilder = PubsubMessage.newBuilder()
                    .setData(data)
                    .putAttributes("type", event.getClass().getName());
            pubSubUtil.keyFor(event).ifPresent(messageBuilder::setOrderingKey);
            pubSubTemplate.publish(qualifiedTopic, messageBuilder.build()).join();
        }
    }
}
