package be.appify.prefab.core.sns;

/**
 * Subscribes consumers to SNS topics through SQS queues. Generated subscribers depend on this interface rather than
 * on {@link SqsUtil}, so another transport, such as the in-memory bus of {@code prefab-test}, can replace the real one.
 */
public interface SqsSubscriptions {

    /**
     * Subscribes to an SNS topic via an SQS queue.
     *
     * @param request the subscription request containing queue name, topic, type, and consumer
     * @param <T>     the type of the messages
     * @return a handle that stops the subscription
     */
    <T> SqsSubscription subscribe(SqsSubscriptionRequest<T> request);
}
