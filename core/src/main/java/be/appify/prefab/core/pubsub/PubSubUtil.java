package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.spring.JsonUtil;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.protobuf.Duration;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.RetryPolicy;
import com.google.pubsub.v1.Subscription;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
@ConditionalOnClass(PubSubAdmin.class)
public class PubSubUtil {
    private static final Logger log = LoggerFactory.getLogger(PubSubUtil.class);
    private final String projectId;
    private final Integer maxRetries;
    private final Integer minimumBackoff;
    private final Integer maximumBackoff;
    private final PubSubAdmin pubSubAdmin;
    private final PubSubSubscriberTemplate subscriberTemplate;
    private final JsonUtil jsonUtil;
    private final ConcurrentMap<String, Class<?>> messageTypes = new ConcurrentHashMap<>();
    private final String dltTopicName;

    public PubSubUtil(
            @Value("${spring.cloud.gcp.project-id}") String projectId,
            @Value("${spring.application.name}") String applicationName,
            @Value("${prefab.pubsub.dlt.topic.name:}") String dltTopicName,
            @Value("${prefab.pubsub.dlt.retries.limit:5}") Integer maxRetries,
            @Value("${prefab.pubsub.dlt.retries.minimum-backoff-ms:1000}") Integer minimumBackoff,
            @Value("${prefab.pubsub.dlt.retries.maximum-backoff-ms:30000}") Integer maximumBackoff,
            PubSubAdmin pubSubAdmin,
            PubSubSubscriberTemplate subscriberTemplate,
            JsonUtil jsonUtil
    ) {
        this.projectId = projectId;
        this.maxRetries = maxRetries;
        this.minimumBackoff = minimumBackoff;
        this.maximumBackoff = maximumBackoff;
        this.pubSubAdmin = pubSubAdmin;
        this.subscriberTemplate = subscriberTemplate;
        this.jsonUtil = jsonUtil;
        this.dltTopicName = !isEmpty(dltTopicName) ? dltTopicName : applicationName + ".dlt";
    }

    public <T> void subscribe(
            String topic,
            String subscription,
            Class<T> type,
            Consumer<T> consumer
    ) {
        subscribe(topic, subscription, type, consumer, Runnable::run);
    }

    public <T> void subscribe(
            String topic,
            String subscription,
            Class<T> type,
            Consumer<T> consumer,
            Executor executor
    ) {
        var topicName = ensureTopicExists(topic);
        var subscriptionName = ensureSubscriptionExists(subscription, topicName, true);
        subscriberTemplate.subscribe(subscriptionName, message ->
                executor.execute(() -> consume(type, consumer, message)));
    }

    private <T> void consume(Class<T> type, Consumer<T> consumer, BasicAcknowledgeablePubsubMessage message) {
        var pubsubMessage = message.getPubsubMessage();
        try {
            if (pubsubMessage.containsAttributes("type")) {
                consumeTyped(type, consumer, pubsubMessage);
            } else {
                consumer.accept(jsonUtil.parseJson(pubsubMessage.getData().toStringUtf8(), type));
            }
            message.ack(); // TODO: delivery semantics
        } catch (Exception e) {
            log.error("Error processing Pub/Sub message: {}", pubsubMessage.getData().toStringUtf8(), e);
            message.nack();
            throw e;
        }
    }

    private <T> void consumeTyped(Class<T> type, Consumer<T> consumer, PubsubMessage pubsubMessage) {
        var typeName = pubsubMessage.getAttributesOrThrow("type");
        var consumedType = messageTypes.computeIfAbsent(typeName, key -> {
            try {
                return Class.forName(typeName);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Could not find class for type found in message: " + typeName, e);
            }
        });
        if (type.isAssignableFrom(consumedType)) {
            consumer.accept(jsonUtil.parseJson(pubsubMessage.getData().toStringUtf8(), type));
        }
    }

    public String ensureTopicExists(String topic) {
        var topicName = ProjectTopicName.of(projectId, topic).toString();
        try {
            if (pubSubAdmin.getTopic(topicName) == null) {
                pubSubAdmin.createTopic(topicName);
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create topic [%s], make sure Pub/Sub is available at the specified endpoint".formatted(
                            topicName), e);
        }
        return topicName;
    }

    public void deleteAllSubscriptions() {
        pubSubAdmin.listSubscriptions().forEach(subscription -> {
            var subscriptionName = subscription.getName();
            pubSubAdmin.deleteSubscription(subscriptionName);
        });
    }

    public void deleteAllTopics() {
        pubSubAdmin.listTopics().forEach(topic -> {
            var topicName = topic.getName();
            pubSubAdmin.deleteTopic(topicName);
        });
    }

    private String ensureSubscriptionExists(
            String subscription,
            String fullyQualifiedTopic,
            boolean withDeadLetterPolicy
    ) {
        var subscriptionName = ProjectSubscriptionName.of(projectId, subscription).toString();
        if (pubSubAdmin.getSubscription(subscriptionName) == null) {
            var subscriptionBuilder = Subscription.newBuilder()
                    .setName(subscriptionName)
                    .setTopic(fullyQualifiedTopic)
                    .setEnableMessageOrdering(true);
            if (withDeadLetterPolicy) {
                var deadLetterTopic = ensureTopicExists(dltTopicName);
                ensureSubscriptionExists(dltTopicName + "-on-error", deadLetterTopic, false);
                subscriptionBuilder.setDeadLetterPolicy(DeadLetterPolicy.newBuilder()
                        .setDeadLetterTopic(deadLetterTopic)
                        .setMaxDeliveryAttempts(maxRetries)
                        .build());
                subscriptionBuilder.setRetryPolicy(RetryPolicy.newBuilder()
                        .setMinimumBackoff(toDuration(minimumBackoff))
                        .setMaximumBackoff(toDuration(maximumBackoff)));

            }
            pubSubAdmin.createSubscription(subscriptionBuilder);
        }
        return subscriptionName;
    }

    private @NonNull Duration toDuration(Integer duration) {
        return Duration.newBuilder()
                .setSeconds(duration / 1000)
                .setNanos((duration % 1000) * 1000000)
                .build();
    }

    public void deleteSubscription(String subscription) {
        var subscriptionName = ProjectSubscriptionName.of(projectId, subscription).toString();
        if (pubSubAdmin.getSubscription(subscriptionName) != null) {
            pubSubAdmin.deleteSubscription(subscriptionName);
        }
    }
}
