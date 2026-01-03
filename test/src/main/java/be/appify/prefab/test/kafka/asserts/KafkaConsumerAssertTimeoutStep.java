package be.appify.prefab.test.kafka.asserts;

import java.util.concurrent.TimeUnit;

/**
 * Step for asserting messages received by a Kafka consumer within a specific timeout.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public interface KafkaConsumerAssertTimeoutStep<K, V> {
    /**
     * Asserts that the consumer has received messages within a specific timeout.
     *
     * @param timeout  the maximum time to wait
     * @param timeUnit the unit of time for the timeout
     * @return the next step in the assertion chain
     */
    KafkaConsumerAssertWhereStep<K, V> within(long timeout, TimeUnit timeUnit);
}
