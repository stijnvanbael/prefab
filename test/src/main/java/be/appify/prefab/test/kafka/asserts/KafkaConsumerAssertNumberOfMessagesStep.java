package be.appify.prefab.test.kafka.asserts;

import java.util.concurrent.TimeUnit;

public interface KafkaConsumerAssertNumberOfMessagesStep<K, V> {
    /// Asserts that the consumer has received at least a specific number of messages.
    KafkaConsumerAssertTimeoutStep<K, V> hasReceivedMessages(int numberOfMessages);

    KafkaConsumerAssertWhereStep<K, V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit);
}
