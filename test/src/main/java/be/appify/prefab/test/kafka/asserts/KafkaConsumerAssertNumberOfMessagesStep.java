package be.appify.prefab.test.kafka.asserts;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public interface KafkaConsumerAssertNumberOfMessagesStep<K, V> {
    /// Asserts that the consumer has received at least a specific number of messages.
    KafkaConsumerAssertTimeoutStep<K, V> hasReceivedMessages(int numberOfMessages);

    /// Asserts that the consumer has received messages within a specific timeout.
    KafkaConsumerAssertWhereStep<K, V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit);

    /// Asserts that the consumer has received a message with a value satisfying the given requirements.
    default <T> void hasReceivedValueSatisfying(Class<T> type, Consumer<T> requirements) {
        hasReceivedMessagesWithin(5, TimeUnit.SECONDS)
                .where(records -> records.anySatisfy(record ->
                        assertThat(record.value()).isInstanceOfSatisfying(type, requirements)));
    }
}
