package be.appify.prefab.test.kafka.asserts;

import java.util.concurrent.TimeUnit;

public interface KafkaConsumerAssertTimeoutStep<K, V> {
    /// Asserts that the consumer has received messages within a specific timeout.
    KafkaConsumerAssertWhereStep<K, V> within(long timeout, TimeUnit timeUnit);
}
