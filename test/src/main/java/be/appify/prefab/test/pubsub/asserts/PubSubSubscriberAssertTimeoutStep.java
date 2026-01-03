package be.appify.prefab.test.pubsub.asserts;

import java.util.concurrent.TimeUnit;

/**
 * Asserts messages received by the subscriber within a specific timeout.
 *
 * @param <V> the type of messages
 */
public interface PubSubSubscriberAssertTimeoutStep<V> {
    /**
     * Specifies the timeout within which the messages should have been received.
     *
     * @param timeout  the timeout duration
     * @param timeUnit the time unit of the timeout
     * @return the next step in the assertion chain
     */
    PubSubSubscriberAssertWhereStep<V> within(long timeout, TimeUnit timeUnit);
}
