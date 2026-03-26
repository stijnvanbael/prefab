package be.appify.prefab.test.asserts;

import java.util.concurrent.TimeUnit;

/**
 * Asserts events received by the consumer within a specific timeout.
 *
 * @param <V> the type of events
 */
public interface EventConsumerTimeoutStep<V> {
    /**
     * Specifies the timeout within which the events should have been received.
     *
     * @param timeout  the timeout duration
     * @param timeUnit the time unit of the timeout
     * @return the next step in the assertion chain
     */
    EventConsumerWhereStep<V> within(long timeout, TimeUnit timeUnit);
}
