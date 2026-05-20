package be.appify.prefab.core.pubsub;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Generic Pub/Sub publisher that publishes any Spring application event whose type is registered
 * in {@link PubSubUtil} to the corresponding Pub/Sub topic.
 */
@Component
@ConditionalOnClass(PubSubAdmin.class)
public class GenericPubSubPublisher {
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

    @EventListener
    public void publish(Object event) {
        var topic = pubSubUtil.tryTopicForType(event.getClass());
        if (topic.isEmpty()) {
            log.trace("Event type {} not registered in PubSubUtil, skipping", event.getClass().getName());
            return;
        }
        var resolvedTopic = topic.get();
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
