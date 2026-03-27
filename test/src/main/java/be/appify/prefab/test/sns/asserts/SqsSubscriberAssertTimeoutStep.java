package be.appify.prefab.test.sns.asserts;

import java.util.concurrent.TimeUnit;

/**
 * Asserts messages received by the SQS subscriber within a specific timeout.
 *
 * @param <V> the type of messages
 * @deprecated Use {@link be.appify.prefab.test.asserts.EventConsumerTimeoutStep} instead.
 */
@Deprecated
public interface SqsSubscriberAssertTimeoutStep<V> {
    /**
     * Specifies the timeout within which the messages should have been received.
     *
     * @param timeout  the timeout duration
     * @param timeUnit the time unit of the timeout
     * @return the next step in the assertion chain
     */
    SqsSubscriberAssertWhereStep<V> within(long timeout, TimeUnit timeUnit);
}
