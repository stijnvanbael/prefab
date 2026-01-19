package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.annotations.EventHandlerConfig;
import com.google.pubsub.v1.DeadLetterPolicy;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Request to subscribe to a Pub/Sub topic.
 *
 * @param <T> The type of the event to subscribe to.
 */
public class SubscribeRequest<T> {
    public static final DeadLetterPolicy DEFAULT_DEAD_LETTER_POLICY = DeadLetterPolicy.newBuilder()
            .setDeadLetterTopic(EventHandlerConfig.DEFAULT_DEAD_LETTER_TOPIC)
            .build();
    private final String topic;
    private final String subscription;
    private final Class<T> type;
    private final Consumer<T> consumer;
    private Executor executor = Runnable::run;
    private DeadLetterPolicy deadLetterPolicy = DEFAULT_DEAD_LETTER_POLICY;

    /**
     * Creates a new subscribe request.
     *
     * @param topic        The topic to subscribe to.
     * @param subscription The subscription name.
     * @param type         The type of the event to subscribe to.
     * @param consumer     The consumer to handle the events.
     */
    public SubscribeRequest(
            String topic,
            String subscription,
            Class<T> type,
            Consumer<T> consumer
    ) {
        this.topic = topic;
        this.subscription = subscription;
        this.type = type;
        this.consumer = consumer;
    }

    /**
     * Gets the topic to subscribe to.
     *
     * @return The topic.
     */
    public String topic() {
        return topic;
    }

    /**
     * Gets the subscription name.
     *
     * @return The subscription name.
     */
    public String subscription() {
        return subscription;
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
     * Sets the executor to run the consumer.
     *
     * @param executor The executor.
     * @return The subscribe request.
     */
    public SubscribeRequest<T> withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Gets the dead letter policy.
     *
     * @return The dead letter policy.
     */
    public DeadLetterPolicy deadLetterPolicy() {
        return deadLetterPolicy;
    }

    /**
     * Sets the dead letter policy.
     *
     * @param deadLetterPolicy The dead letter policy.
     * @return The subscribe request.
     */
    public SubscribeRequest<T> withDeadLetterPolicy(DeadLetterPolicy deadLetterPolicy) {
        this.deadLetterPolicy = deadLetterPolicy;
        return this;
    }

    /**
     * Checks if the default dead letter policy is used.
     *
     * @return True if the default dead letter policy is used, false otherwise.
     */
    public boolean isUsingDefaultDeadLetterPolicy() {
        return DEFAULT_DEAD_LETTER_POLICY.equals(deadLetterPolicy);
    }
}
