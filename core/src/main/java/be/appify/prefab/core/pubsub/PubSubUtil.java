package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.protobuf.Duration;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.RetryPolicy;
import com.google.pubsub.v1.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
public class PubSubUtil implements PubSubSubscriptions {
    private static final Logger log = LoggerFactory.getLogger(PubSubUtil.class);
    private final String projectId;
    private final Integer maxRetries;
    private final Integer minimumBackoff;
    private final Integer maximumBackoff;
    private final PubSubAdmin pubSubAdmin;
    private final PubSubSubscriberTemplate subscriberTemplate;
    private final PubSubMessageDecoder decoder;
    private final EventRegistry eventRegistry;
    private final String deadLetterTopicName;
    private final RetryTemplate retryTemplate;
    private final List<ActiveSubscription> activeSubscriptions = new CopyOnWriteArrayList<>();
    private final AtomicInteger inFlightMessages = new AtomicInteger();

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
        this.decoder = new PubSubMessageDecoder(deserializer, eventRegistry);
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

    @Override
    public <T> void subscribe(SubscriptionRequest<T> request) {
        subscribeWithHandle(request);
    }

    /**
     * Subscribes with the normal provisioning and retry policy and returns a handle for stopping delivery.
     *
     * @param request subscription details
     * @param <T> message type
     * @return the running subscriber
     */
    public <T> Subscriber subscribeWithHandle(SubscriptionRequest<T> request) {
        var topicName = ensureTopicExists(request.topic());
        var deadLetterPolicy =
                request.isUsingDefaultDeadLetterPolicy() ? deadLetterPolicy(deadLetterTopicName) : request.deadLetterPolicy();
        var subscriptionName = ensureSubscriptionExists(request.subscription(), topicName, deadLetterPolicy);
        return doSubscribe(request, subscriptionName);
    }

    private <T> Subscriber doSubscribe(SubscriptionRequest<T> request, String subscriptionName) {
        var subscriber = subscriberTemplate.subscribe(subscriptionName, message -> dispatch(request, message));
        activeSubscriptions.add(new ActiveSubscription(request, request.subscription(), new AtomicReference<>(subscriber)));
        return subscriber;
    }

    /**
     * Hands a received message to the subscription's executor. A message can never be lost or terminate the
     * subscriber: if it cannot be handed off, it is negatively acknowledged and nothing is rethrown.
     */
    private <T> void dispatch(SubscriptionRequest<T> request, BasicAcknowledgeablePubsubMessage message) {
        inFlightMessages.incrementAndGet();
        try {
            request.executor().execute(() -> {
                try {
                    consume(request, message);
                } finally {
                    inFlightMessages.decrementAndGet();
                }
            });
        } catch (Throwable e) {
            inFlightMessages.decrementAndGet();
            log.warn("Could not hand off Pub/Sub message for processing: {}", message.getPubsubMessage(), e);
            nackQuietly(message);
        }
    }

    /**
     * Resets every active subscription for test isolation: stops delivery, waits for in-flight messages to settle,
     * discards any unprocessed backlog, and recreates the subscription empty and re-subscribed.
     * <p>
     * Production processing semantics are unchanged; this only drops messages that were never delivered yet, so
     * events published by earlier tests cannot leak into later ones once their targets have been removed.
     */
    public void resetSubscriptions() {
        var snapshot = List.copyOf(activeSubscriptions);
        snapshot.forEach(active -> stopQuietly(active.subscriber().get()));
        awaitQuiescent();
        activeSubscriptions.clear();
        for (var active : snapshot) {
            try {
                deleteSubscription(active.subscription());
                subscribeWithHandle(active.request());
            } catch (Throwable e) {
                log.warn("Could not reset Pub/Sub subscription [{}]: {}", active.subscription(), e.getMessage(), e);
            }
        }
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
     * Checks registration without requiring the event to have exactly one topic.
     *
     * @param type event class
     * @return whether any binding of the event is registered
     */
    public boolean hasTopicForType(Class<?> type) {
        return eventRegistry.hasTopicForType(type);
    }

    /**
     * Checks whether a concrete binding is registered.
     *
     * @param type concrete event binding
     * @return whether the binding has registered topics
     */
    public boolean hasTopicForType(EventType<?> type) {
        return !eventRegistry.topicsForType(type).isEmpty();
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

    /**
     * Processes one message. Consumer success is acknowledged exactly once, outside the retryable, so a successful
     * consumer is never invoked again. Any failure after the retry policy is exhausted hands the message back to
     * Pub/Sub with a negative acknowledgement: it is redelivered and, after the configured number of attempts,
     * routed to the dead-letter topic. Nothing is rethrown; a poison message can never terminate or starve the
     * subscriber. Genuinely missing projections therefore keep their fail-and-retry-and-dead-letter behavior.
     */
    private <T> void consume(SubscriptionRequest<T> request, BasicAcknowledgeablePubsubMessage message) {
        var pubsubMessage = message.getPubsubMessage();
        try {
            request.retryTemplate().orElse(this.retryTemplate).execute(() -> {
                try {
                    if (request.rawConsumer().isPresent()) {
                        request.rawConsumer().orElseThrow().accept(pubsubMessage);
                    } else {
                        decoder.decode(request, pubsubMessage).ifPresent(request.consumer());
                    }
                } catch (Exception e) {
                    log.warn("Error processing Pub/Sub message: {}, cause: {}", truncate(pubsubMessage.getData().toStringUtf8(), 200),
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
                    throw e;
                }
                return null;
            });
            ackQuietly(message);
        } catch (Throwable e) {
            // Not rethrown: the listener runs on an executor, so nobody can handle the exception. The nack hands the
            // message back to Pub/Sub, which redelivers it or routes it to the dead-letter topic.
            log.error("Retries exhausted when processing Pub/Sub message: {}", message, e);
            nackQuietly(message);
        }
    }

    private static void ackQuietly(BasicAcknowledgeablePubsubMessage message) {
        try {
            message.ack();
        } catch (Throwable e) {
            log.warn("Could not acknowledge Pub/Sub message: {}", message.getPubsubMessage(), e);
        }
    }

    private static void nackQuietly(BasicAcknowledgeablePubsubMessage message) {
        try {
            message.nack();
        } catch (Throwable e) {
            log.warn("Could not negatively acknowledge Pub/Sub message: {}", message.getPubsubMessage(), e);
        }
    }

    private static void stopQuietly(Subscriber subscriber) {
        if (subscriber == null) {
            return;
        }
        try {
            subscriber.stopAsync();
            subscriber.awaitTerminated(5, TimeUnit.SECONDS);
        } catch (Throwable e) {
            log.warn("Could not stop Pub/Sub subscriber [{}]: {}", subscriber.getSubscriptionNameString(), e.getMessage(), e);
        }
    }

    private void awaitQuiescent() {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (inFlightMessages.get() > 0 && System.nanoTime() < deadlineNanos) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** A live subscription, retained so test isolation can stop and recreate it without restarting the application. */
    private record ActiveSubscription(
            SubscriptionRequest<?> request,
            String subscription,
            AtomicReference<Subscriber> subscriber) {}

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
