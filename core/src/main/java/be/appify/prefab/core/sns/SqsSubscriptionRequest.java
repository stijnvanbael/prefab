package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.domain.EventType;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.springframework.core.retry.RetryTemplate;

/**
 * Request to subscribe to an SQS queue backed by an SNS topic.
 *
 * @param <T>
 *         The type of the event to subscribe to.
 */
public class SqsSubscriptionRequest<T> {
    /** The default dead letter queue name sentinel to use if none is provided. */
    public static final String DEFAULT_DEAD_LETTER_QUEUE_NAME = EventHandlerConfig.DEFAULT_DEAD_LETTER_TOPIC;

    private final String topic;
    private final String queueName;
    private final Class<T> type;
    private final Consumer<T> consumer;
    private final Optional<EventType<T>> eventType;
    private RetryTemplate retryTemplate = null;
    private Executor executor = Runnable::run;
    private String deadLetterQueueName = DEFAULT_DEAD_LETTER_QUEUE_NAME;
    private SqsMessageConsumer<T> messageConsumer = null;
    private Duration visibilityTimeout = null;
    private boolean fifo = false;

    /**
     * Creates a new SQS subscription request.
     *
     * @param topic
     *         The SNS topic name.
     * @param queueName
     *         The SQS queue name.
     * @param type
     *         The type of the event to subscribe to.
     * @param consumer
     *         The consumer to handle the events.
     */
    public SqsSubscriptionRequest(
            String topic,
            String queueName,
            Class<T> type,
            Consumer<T> consumer
    ) {
        this.topic = topic;
        this.queueName = queueName;
        this.type = type;
        this.consumer = consumer;
        this.eventType = Optional.empty();
    }

    /**
     * Creates a request for one concrete generic event binding.
     *
     * @param topic source topic
     * @param queueName independent queue name
     * @param type concrete event token
     * @param consumer event consumer
     */
    @SuppressWarnings("unchecked")
    public SqsSubscriptionRequest(String topic, String queueName, EventType<T> type, Consumer<T> consumer) {
        this.topic = topic;
        this.queueName = queueName;
        this.type = (Class<T>) type.rawType();
        this.consumer = consumer;
        this.eventType = Optional.of(type);
    }

    /** @return the concrete binding, empty for legacy class-based subscriptions */
    public Optional<EventType<T>> eventType() {
        return eventType;
    }

    /**
     * Gets the SNS topic name.
     *
     * @return The topic.
     */
    public String topic() {
        return topic;
    }

    /**
     * Gets the SQS queue name.
     *
     * @return The queue name.
     */
    public String queueName() {
        return queueName;
    }

    /**
     * Gets the type of the event to subscribe to.
     *
     * @return The event type.
     */
    public Class<T> type() {
        return type;
    }

    /**
     * Gets the consumer to handle the events.
     *
     * @return The consumer.
     */
    public Consumer<T> consumer() {
        return consumer;
    }

    /**
     * Gets the executor to run the consumer.
     *
     * @return The executor.
     */
    public Executor executor() {
        return executor;
    }

    /**
     * Gets the retry template for handling retries.
     *
     * @return An optional retry template.
     */
    public Optional<RetryTemplate> retryTemplate() {
        return Optional.ofNullable(retryTemplate);
    }

    /**
     * Gets the dead letter queue name.
     *
     * @return The dead letter queue name, or null if dead lettering is disabled.
     */
    public String deadLetterQueueName() {
        return deadLetterQueueName;
    }

    /**
     * Sets the executor to run the consumer.
     *
     * @param executor
     *         The executor.
     * @return The subscription request.
     */
    public SqsSubscriptionRequest<T> withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Sets the dead letter queue name. Pass {@code null} to disable dead lettering.
     *
     * @param deadLetterQueueName
     *         The dead letter queue name, or null to disable dead lettering.
     * @return The subscription request.
     */
    public SqsSubscriptionRequest<T> withDeadLetterQueueName(String deadLetterQueueName) {
        this.deadLetterQueueName = deadLetterQueueName;
        return this;
    }

    /**
     * Sets the retry template for handling retries.
     *
     * @param retryTemplate
     *         The retry template.
     * @return The subscription request.
     */
    public SqsSubscriptionRequest<T> withRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
        return this;
    }

    /**
     * Gets the consumer that receives message metadata and acknowledges messages itself.
     *
     * @return the consumer, or empty when the plain consumer handles the events
     */
    public Optional<SqsMessageConsumer<T>> messageConsumer() {
        return Optional.ofNullable(messageConsumer);
    }

    /**
     * Gets the visibility timeout of the queue.
     *
     * @return the timeout, or empty to keep the queue's own setting
     */
    public Optional<Duration> visibilityTimeout() {
        return Optional.ofNullable(visibilityTimeout);
    }

    /**
     * Returns whether the subscription requires a FIFO topic and queue regardless of the registered event.
     *
     * @return whether FIFO is required
     */
    public boolean fifo() {
        return fifo;
    }

    /**
     * Handles deliveries with a consumer that receives the message metadata and decides when the message is
     * deleted, instead of deleting it after the plain consumer returns.
     *
     * @param messageConsumer the metadata-aware consumer
     * @return the subscription request
     */
    public SqsSubscriptionRequest<T> withMessageConsumer(SqsMessageConsumer<T> messageConsumer) {
        this.messageConsumer = messageConsumer;
        return this;
    }

    /**
     * Sets the visibility timeout of the queue. It must exceed the time that handling a message can take,
     * including retries.
     *
     * @param visibilityTimeout the timeout, with a resolution of one second
     * @return the subscription request
     */
    public SqsSubscriptionRequest<T> withVisibilityTimeout(Duration visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
        return this;
    }

    /**
     * Requires a FIFO topic and queue, also when the event has no partitioning key.
     *
     * @param fifo whether FIFO is required
     * @return the subscription request
     */
    public SqsSubscriptionRequest<T> withFifo(boolean fifo) {
        this.fifo = fifo;
        return this;
    }

    /**
     * Checks if the default dead letter queue name is used.
     *
     * @return True if the default dead letter queue name is used, false otherwise.
     */
    public boolean isUsingDefaultDeadLetterQueueName() {
        return DEFAULT_DEAD_LETTER_QUEUE_NAME.equals(deadLetterQueueName);
    }
}
