package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.EventHandlerConfig;
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
    private RetryTemplate retryTemplate = null;
    private Executor executor = Runnable::run;
    private String deadLetterQueueName = DEFAULT_DEAD_LETTER_QUEUE_NAME;

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
     * Checks if the default dead letter queue name is used.
     *
     * @return True if the default dead letter queue name is used, false otherwise.
     */
    public boolean isUsingDefaultDeadLetterQueueName() {
        return DEFAULT_DEAD_LETTER_QUEUE_NAME.equals(deadLetterQueueName);
    }
}
