package be.appify.prefab.core.pubsub;

import com.fasterxml.jackson.annotation.JsonSubTypes;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Utility class for managing Pub/Sub topics, subscriptions, and for subscribing to messages with optional dead-letter handling.
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
    private final ConcurrentMap<String, Class<?>> allowedTypes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, String> typeToTopic = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Function<Object, String>> keyExtractors = new ConcurrentHashMap<>();
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
     * @param backoffMultiplier
     *         the backoff multiplier
     * @param pubSubAdmin
     *         the Pub/Sub admin client
     * @param subscriberTemplate
     *         the Pub/Sub subscriber template
     * @param deserializer
     *         the Pub/Sub deserializer
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
            PubSubDeserializer deserializer
    ) {
        this.projectId = projectId;
        this.maxRetries = maxRetries;
        this.minimumBackoff = minimumBackoff;
        this.maximumBackoff = maximumBackoff;
        this.pubSubAdmin = pubSubAdmin;
        this.subscriberTemplate = subscriberTemplate;
        this.deserializer = deserializer;
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
        subscribe(new SubscriptionRequest<>(topic, subscription, type, consumer));
    }

    /**
     * Registers an event type in the allowlist for safe deserialization from the Pub/Sub message {@code type} attribute.
     * Permitted subtypes of sealed interfaces and {@code @JsonSubTypes}-annotated subtypes are registered recursively.
     *
     * @param typeName
     *         the fully-qualified class name that may appear as the Pub/Sub message {@code type} attribute
     * @param type
     *         the corresponding Java class
     */
    public void registerType(String typeName, Class<?> type) {
        allowedTypes.put(typeName, type);
        var permittedSubclasses = type.getPermittedSubclasses();
        if (permittedSubclasses != null) {
            for (var subclass : permittedSubclasses) {
                registerType(subclass.getName(), subclass);
            }
        }
        var jsonSubTypes = type.getAnnotation(JsonSubTypes.class);
        if (jsonSubTypes != null) {
            for (var subType : jsonSubTypes.value()) {
                registerType(subType.value().getName(), subType.value());
            }
        }
    }

    /**
     * Registers a topic and optional ordering-key extractor for an event type so the generic publisher can resolve
     * them at runtime. Permitted subtypes of sealed interfaces are registered recursively.
     *
     * @param topic        the simple Pub/Sub topic name
     * @param type         the Java class of the event
     * @param keyExtractor a function that returns the ordering key for a given event instance, or {@code null} if none
     */
    @SuppressWarnings("unchecked")
    public <E> void registerEventTopic(String topic, Class<E> type, Function<E, String> keyExtractor) {
        typeToTopic.put(type, topic);
        if (keyExtractor != null) {
            keyExtractors.put(type, (Function<Object, String>) (Function<?, String>) keyExtractor);
        }
        if (type.isSealed()) {
            for (var subtype : type.getPermittedSubclasses()) {
                registerEventTopic(topic, (Class<Object>) subtype, null);
            }
        }
    }

    /**
     * Registers a topic for an event type without an ordering-key extractor.
     *
     * @param topic the simple Pub/Sub topic name
     * @param type  the Java class of the event
     */
    public void registerEventTopic(String topic, Class<?> type) {
        registerEventTopic(topic, type, null);
    }

    /**
     * Resolves the simple Pub/Sub topic for a given event type.
     *
     * @param type the event class
     * @return the registered topic name
     * @throws IllegalArgumentException if no topic is registered for the type
     */
    public String topicForType(Class<?> type) {
        return tryTopicForType(type)
                .orElseThrow(() -> new IllegalArgumentException("No topic registered for type: " + type.getName()));
    }

    /**
     * Resolves the simple Pub/Sub topic for a given event type if one is registered.
     *
     * @param type the event class
     * @return the registered topic name if found
     * @throws IllegalStateException if multiple equally specific topics match the type
     */
    public Optional<String> tryTopicForType(Class<?> type) {
        var topic = typeToTopic.get(type);
        if (topic != null) {
            return Optional.of(topic);
        }
        String selectedTopic = null;
        Integer selectedDistance = null;
        for (var entry : typeToTopic.entrySet()) {
            if (!entry.getKey().isAssignableFrom(type)) {
                continue;
            }
            var distance = hierarchyDistance(type, entry.getKey());
            if (distance.isEmpty()) {
                continue;
            }
            if (selectedDistance == null || distance.get() < selectedDistance) {
                selectedDistance = distance.get();
                selectedTopic = entry.getValue();
            } else if (distance.get().equals(selectedDistance) && !entry.getValue().equals(selectedTopic)) {
                throw new IllegalStateException(
                        "Ambiguous topics registered for type %s: %s and %s"
                                .formatted(type.getName(), selectedTopic, entry.getValue())
                );
            }
        }
        return Optional.ofNullable(selectedTopic);
    }

    private static Optional<Function<Object, String>> tryExtractorForType(
            Class<?> type,
            ConcurrentMap<Class<?>, Function<Object, String>> extractors
    ) {
        var extractor = extractors.get(type);
        if (extractor != null) {
            return Optional.of(extractor);
        }
        Function<Object, String> selectedExtractor = null;
        Integer selectedDistance = null;
        for (var entry : extractors.entrySet()) {
            if (!entry.getKey().isAssignableFrom(type)) {
                continue;
            }
            var distance = hierarchyDistance(type, entry.getKey());
            if (distance.isEmpty()) {
                continue;
            }
            if (selectedDistance == null || distance.get() < selectedDistance) {
                selectedDistance = distance.get();
                selectedExtractor = entry.getValue();
            } else if (distance.get().equals(selectedDistance) && !entry.getValue().equals(selectedExtractor)) {
                throw new IllegalStateException("Ambiguous key extractors registered for type: " + type.getName());
            }
        }
        return Optional.ofNullable(selectedExtractor);
    }

    private static Optional<Integer> hierarchyDistance(Class<?> source, Class<?> target) {
        if (source.equals(target)) {
            return Optional.of(0);
        }
        if (!target.isAssignableFrom(source)) {
            return Optional.empty();
        }
        var queue = new java.util.ArrayDeque<Class<?>>();
        var distances = new java.util.HashMap<Class<?>, Integer>();
        queue.add(source);
        distances.put(source, 0);
        while (!queue.isEmpty()) {
            var current = queue.remove();
            var distance = distances.get(current);
            if (current.equals(target)) {
                return Optional.of(distance);
            }
            var superclass = current.getSuperclass();
            if (superclass != null && distances.putIfAbsent(superclass, distance + 1) == null) {
                queue.add(superclass);
            }
            for (var iface : current.getInterfaces()) {
                if (distances.putIfAbsent(iface, distance + 1) == null) {
                    queue.add(iface);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the ordering key for an event, if a key extractor has been registered for its type.
     *
     * @param event the event instance
     * @return an {@link Optional} containing the ordering key, or empty if none is registered
     */
    public Optional<String> keyFor(Object event) {
        return tryExtractorForType(event.getClass(), keyExtractors).map(extractor -> extractor.apply(event));
    }

    /**
     * Subscribes to a Pub/Sub topic using the provided subscription request.
     *
     * @param request
     *         the subscribe request containing subscription details
     * @param <T>
     *         the type of the messages
     */
    public <T> void subscribe(SubscriptionRequest<T> request) {
        var topicName = ensureTopicExists(request.topic());
        var subscriptionName = ensureSubscriptionExists(
                request.subscription(),
                topicName,
                request.isUsingDefaultDeadLetterPolicy() ? deadLetterPolicy(deadLetterTopicName) : request.deadLetterPolicy()
        );
        subscriberTemplate.subscribe(subscriptionName, message ->
                request.executor().execute(
                        () -> consume(request, message)));
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
        var consumedType = allowedTypes.get(typeName);
        if (consumedType == null) {
            throw new IllegalArgumentException("Type not registered in allowlist: " + typeName);
        }
        if (request.type().isAssignableFrom(consumedType)) {
            request.consumer().accept(deserializer.deserialize(request.topic(), pubsubMessage.getData(), request.type()));
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
     * Extracts the simple topic name from a fully qualified topic name.
     *
     * @param fullyQualifiedTopic
     *         the fully qualified topic name (e.g., "projects/my-project/topics/my-topic")
     * @return the simple topic name (e.g., "my-topic")
     */
    public static String simpleTopicName(String fullyQualifiedTopic) {
        return fullyQualifiedTopic.substring(fullyQualifiedTopic.lastIndexOf("/") + 1);
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

    private static String truncate(String body, int maxLength) {
        if (body == null) {
            return "<null>";
        }
        return body.length() <= maxLength ? body : body.substring(0, maxLength) + "...[truncated " + (body.length() - maxLength) + " chars]";
    }
}
