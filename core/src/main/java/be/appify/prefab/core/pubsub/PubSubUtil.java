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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Utility class for managing Pub/Sub topics and subscriptions, and for subscribing to messages with optional dead-letter handling.
 */
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
    private final String deadLetterTopicName;
    private final RetryTemplate retryTemplate;

    /**
     * Constructs a new PubSubUtil with the given configuration and dependencies.
     *
     * @param projectId
     *         the GCP project ID
     * @param applicationName
     *         the application name
     * @param deadLetterTopicName
     *         the dead-letter topic name
     * @param maxRetries
     *         the maximum number of retries for dead-letter handling
     * @param minimumBackoff
     *         the minimum backoff time in milliseconds
     * @param maximumBackoff
     *         the maximum backoff time in milliseconds
     * @param pubSubAdmin
     *         the Pub/Sub admin client
     * @param subscriberTemplate
     *         the Pub/Sub subscriber template
     * @param jsonUtil
     *         the JSON utility for serialization/deserialization
     */
    public PubSubUtil(
            @Value("${spring.cloud.gcp.project-id}") String projectId,
            @Value("${spring.application.name}") String applicationName,
            @Value("${prefab.dlt.name:}") String deadLetterTopicName,
            @Value("${prefab.dlt.retries.limit:5}") Integer maxRetries,
            @Value("${prefab.dlt.retries.minimum-backoff-ms:1000}") Integer minimumBackoff,
            @Value("${prefab.dlt.retries.maximum-backoff-ms:30000}") Integer maximumBackoff,
            @Value("${prefab.dlt.retries.multiplier:1.5}") Float backoffMultiplier,
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
        this.deadLetterTopicName = !isEmpty(deadLetterTopicName) ? deadLetterTopicName : applicationName + ".dlt";
        this.retryTemplate = new RetryTemplate(org.springframework.core.retry.RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(java.time.Duration.ofMillis(minimumBackoff))
                .maxDelay(java.time.Duration.ofMillis(maximumBackoff))
                .multiplier(backoffMultiplier)
                .build());
    }

    /**
     * Subscribes to a Pub/Sub topic with the given subscription name and message type, using the provided consumer to process messages.
     *
     * @param topic
     *         the Pub/Sub topic name
     * @param subscription
     *         the subscription name
     * @param type
     *         the class type of the messages
     * @param consumer
     *         the consumer to process messages
     * @param <T>
     *         the type of the messages
     */
    public <T> void subscribe(
            String topic,
            String subscription,
            Class<T> type,
            Consumer<T> consumer
    ) {
        subscribe(new SubscribeRequest<>(topic, subscription, type, consumer));
    }

    /**
     * Subscribes to a Pub/Sub topic using the provided subscribe request.
     *
     * @param request
     *         the subscribe request containing subscription details
     * @param <T>
     *         the type of the messages
     */
    public <T> void subscribe(SubscribeRequest<T> request) {
        var topicName = ensureTopicExists(request.topic());
        var subscriptionName = ensureSubscriptionExists(
                request.subscription(),
                topicName,
                request.isUsingDefaultDeadLetterPolicy() ? deadLetterPolicy(deadLetterTopicName) : request.deadLetterPolicy()
        );
        subscriberTemplate.subscribe(subscriptionName, message ->
                request.executor().execute(
                        () -> consume(request.type(), request.consumer(), request.retryTemplate().orElse(retryTemplate), message)));
    }

    private <T> void consume(Class<T> type, Consumer<T> consumer, RetryTemplate retryTemplate, BasicAcknowledgeablePubsubMessage message) {
        try {
            retryTemplate.execute(() -> {
                var pubsubMessage = message.getPubsubMessage();
                try {
                    if (pubsubMessage.containsAttributes("type")) {
                        consumeTyped(type, consumer, pubsubMessage);
                    } else {
                        consumer.accept(jsonUtil.parseJson(pubsubMessage.getData().toStringUtf8(), type));
                    }
                    message.ack();
                } catch (Exception e) {
                    log.warn("Error processing Pub/Sub message: {}, cause: {}", pubsubMessage.getData().toStringUtf8(),
                            e.getCause().getMessage());
                    throw e;
                }
                return null;
            });
        } catch (RetryException e) {
            message.nack();
            log.error("Retries exhausted when processing Pub/Sub message: {}", message, e);
            throw new RuntimeException(e);
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

    /**
     * Ensures that the specified Pub/Sub topic exists, creating it if necessary.
     *
     * @param topic
     *         the topic name
     * @return the fully qualified topic name
     */
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

    /**
     * Deletes all Pub/Sub subscriptions in the project.
     */
    public void deleteAllSubscriptions() {
        pubSubAdmin.listSubscriptions().forEach(subscription -> {
            var subscriptionName = subscription.getName();
            pubSubAdmin.deleteSubscription(subscriptionName);
        });
    }

    /**
     * Deletes all Pub/Sub topics in the project.
     */
    public void deleteAllTopics() {
        pubSubAdmin.listTopics().forEach(topic -> {
            var topicName = topic.getName();
            pubSubAdmin.deleteTopic(topicName);
        });
    }

    private String ensureSubscriptionExists(
            String subscription,
            String fullyQualifiedTopic,
            DeadLetterPolicy deadLetterPolicy
    ) {
        var subscriptionName = ProjectSubscriptionName.of(projectId, subscription).toString();
        if (pubSubAdmin.getSubscription(subscriptionName) == null) {
            var subscriptionBuilder = Subscription.newBuilder()
                    .setName(subscriptionName)
                    .setTopic(fullyQualifiedTopic)
                    .setEnableMessageOrdering(true);
            if (deadLetterPolicy != null) {
                subscriptionBuilder.setDeadLetterPolicy(deadLetterPolicy);
                subscriptionBuilder.setRetryPolicy(RetryPolicy.newBuilder()
                        .setMinimumBackoff(toDuration(minimumBackoff))
                        .setMaximumBackoff(toDuration(maximumBackoff)));

            }
            pubSubAdmin.createSubscription(subscriptionBuilder);
        }
        return subscriptionName;
    }

    private DeadLetterPolicy deadLetterPolicy(String deadLetterTopicName) {
        var deadLetterTopic = ensureTopicExists(deadLetterTopicName);
        ensureSubscriptionExists(deadLetterTopicName + "-on-error", deadLetterTopic, null);
        return DeadLetterPolicy.newBuilder()
                .setDeadLetterTopic(deadLetterTopic)
                .setMaxDeliveryAttempts(maxRetries)
                .build();
    }

    private Duration toDuration(Integer duration) {
        return Duration.newBuilder()
                .setSeconds(duration / 1000)
                .setNanos((duration % 1000) * 1000000)
                .build();
    }

    /**
     * Deletes the specified Pub/Sub subscription if it exists.
     *
     * @param subscription
     *         the subscription name to delete
     */
    public void deleteSubscription(String subscription) {
        var subscriptionName = ProjectSubscriptionName.of(projectId, subscription).toString();
        if (pubSubAdmin.getSubscription(subscriptionName) != null) {
            pubSubAdmin.deleteSubscription(subscriptionName);
        }
    }
}
