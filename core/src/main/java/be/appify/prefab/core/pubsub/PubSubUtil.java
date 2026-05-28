package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.kafka.EventRegistry;
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
import java.util.List;
import java.util.Optional;
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
 * Utility class for managing Pub/Sub topics, subscriptions, and for subscribing to messages.
 *
 * <p>Topic-to-type mappings, publish-to strategies, and partitioning-key extractors are stored in the
 * shared {@link EventRegistry} so that a single source of truth is maintained across all messaging platforms.
 * Event-type registrars implement {@link be.appify.prefab.core.kafka.EventRegistryCustomizer} and populate
 * the registry directly — there is no separate Pub/Sub-specific registration step.
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
    private final PubSubDeserializer deserializer;
    private final EventRegistry eventRegistry;
    private final String deadLetterTopicName;
    private final RetryTemplate retryTemplate;

    /**
     * Constructs a new PubSubUtil.
     *
     * @param projectId           the GCP project ID
     * @param applicationName     the application name
     * @param deadLetterTopicName the dead-letter topic name
     * @param maxRetries          the maximum number of retries for dead-letter handling
     * @param minimumBackoff      the minimum backoff time in milliseconds
     * @param maximumBackoff      the maximum backoff time in milliseconds
     * @param backoffMultiplier   the backoff multiplier
     * @param pubSubAdmin         the Pub/Sub admin client
     * @param subscriberTemplate  the Pub/Sub subscriber template
     * @param deserializer        the Pub/Sub deserializer
     * @param eventRegistry       the shared event registry
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
            PubSubDeserializer deserializer,
            EventRegistry eventRegistry
    ) {
        this.projectId = projectId;
        this.maxRetries = maxRetries;
        this.minimumBackoff = minimumBackoff;
        this.maximumBackoff = maximumBackoff;
        this.pubSubAdmin = pubSubAdmin;
        this.subscriberTemplate = subscriberTemplate;
        this.deserializer = deserializer;
        this.eventRegistry = eventRegistry;
        this.deadLetterTopicName = !isEmpty(deadLetterTopicName) ? deadLetterTopicName : applicationName + ".dlt";
        this.retryTemplate = new RetryTemplate(org.springframework.core.retry.RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(java.time.Duration.ofMillis(minimumBackoff))
                .maxDelay(java.time.Duration.ofMillis(maximumBackoff))
                .multiplier(backoffMultiplier)
                .build());
    }

    /**
     * Subscribes to a Pub/Sub topic with the given subscription name and message type.
     *
     * @param topic        the Pub/Sub topic name
     * @param subscription the subscription name
     * @param type         the class type of the messages
     * @param consumer     the consumer to process messages
     * @param <T>          the type of the messages
     */
    public <T> void subscribe(String topic, String subscription, Class<T> type, Consumer<T> consumer) {
        subscribe(new SubscriptionRequest<>(topic, subscription, type, consumer));
    }

    /**
     * Subscribes to a Pub/Sub topic using the provided subscription request.
     *
     * @param request the subscribe request containing subscription details
     * @param <T>     the type of the messages
     */
    public <T> void subscribe(SubscriptionRequest<T> request) {
        var topicName = ensureTopicExists(request.topic());
        var subscriptionName = ensureSubscriptionExists(
                request.subscription(),
                topicName,
                request.isUsingDefaultDeadLetterPolicy() ? deadLetterPolicy(deadLetterTopicName) : request.deadLetterPolicy()
        );
        subscriberTemplate.subscribe(subscriptionName, message ->
                request.executor().execute(() -> consume(request, message)));
    }

    /**
     * Resolves the topics to which the given event should be dispatched, applying the registered
     * {@link be.appify.prefab.core.annotations.PublishTo} strategy.
     *
     * @param event the event instance
     * @return the ordered list of target topic names
     */
    public List<String> topicsForDispatch(Object event) {
        return eventRegistry.topicsForDispatch(event);
    }

    /**
     * Resolves the simple Pub/Sub topic for a given event type.
     *
     * @param type the event class
     * @return the registered topic name
     */
    public String topicForType(Class<?> type) {
        return eventRegistry.topicForType(type);
    }

    /**
     * Resolves the simple Pub/Sub topic for a given event type if exactly one is registered.
     *
     * @param type the event class
     * @return the registered topic name if found
     */
    public Optional<String> tryTopicForType(Class<?> type) {
        return eventRegistry.tryTopicForType(type);
    }

    /**
     * Returns the ordering key for an event, if a key extractor has been registered for its type.
     *
     * @param event the event instance
     * @return an {@link Optional} containing the ordering key, or empty if none is registered
     */
    public Optional<String> keyFor(Object event) {
        return eventRegistry.keyFor(event);
    }

    /**
     * Ensures that the specified Pub/Sub topic exists, creating it if necessary.
     *
     * @param topic the topic name
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
                    "Failed to create topic [%s], make sure Pub/Sub is available at the specified endpoint".formatted(topicName), e);
        }
        return topicName;
    }

    /**
     * Extracts the simple topic name from a fully qualified topic name.
     *
     * @param fullyQualifiedTopic the fully qualified topic name
     * @return the simple topic name
     */
    public static String simpleTopicName(String fullyQualifiedTopic) {
        return fullyQualifiedTopic.substring(fullyQualifiedTopic.lastIndexOf("/") + 1);
    }

    /** Deletes all Pub/Sub subscriptions in the project. */
    public void deleteAllSubscriptions() {
        pubSubAdmin.listSubscriptions().forEach(subscription ->
                pubSubAdmin.deleteSubscription(subscription.getName()));
    }

    /** Deletes all Pub/Sub topics in the project. */
    public void deleteAllTopics() {
        pubSubAdmin.listTopics().forEach(topic ->
                pubSubAdmin.deleteTopic(topic.getName()));
    }

    /**
     * Deletes the specified Pub/Sub subscription if it exists.
     *
     * @param subscription the subscription name to delete
     */
    public void deleteSubscription(String subscription) {
        var subscriptionName = ProjectSubscriptionName.of(projectId, subscription).toString();
        if (pubSubAdmin.getSubscription(subscriptionName) != null) {
            pubSubAdmin.deleteSubscription(subscriptionName);
        }
    }

    private <T> void consume(SubscriptionRequest<T> request, BasicAcknowledgeablePubsubMessage message) {
        try {
            request.retryTemplate().orElse(this.retryTemplate).execute(() -> {
                var pubsubMessage = message.getPubsubMessage();
                try {
                    if (pubsubMessage.containsAttributes("type")) {
                        consumeTyped(request, pubsubMessage);
                    } else {
                        request.consumer().accept(deserializer.deserialize(request.topic(), pubsubMessage.getData(), request.type()));
                    }
                    message.ack();
                } catch (Exception e) {
                    log.warn("Error processing Pub/Sub message: {}, cause: {}", truncate(pubsubMessage.getData().toStringUtf8(), 200),
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
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

    private <T> void consumeTyped(SubscriptionRequest<T> request, PubsubMessage pubsubMessage) {
        var typeName = pubsubMessage.getAttributesOrThrow("type");
        var consumedType = eventRegistry.typeByClassName(typeName)
                .orElseThrow(() -> new IllegalArgumentException("Type not registered in allowlist: " + typeName));
        if (request.type().isAssignableFrom(consumedType)) {
            request.consumer().accept(deserializer.deserialize(request.topic(), pubsubMessage.getData(), request.type()));
        }
    }

    private String ensureSubscriptionExists(String subscription, String fullyQualifiedTopic, DeadLetterPolicy deadLetterPolicy) {
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

    private static String truncate(String body, int maxLength) {
        if (body == null) return "<null>";
        return body.length() <= maxLength ? body : body.substring(0, maxLength) + "...[truncated " + (body.length() - maxLength) + " chars]";
    }
}
