package be.appify.prefab.test.kafka.asserts;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step for asserting the number of messages received by a Kafka consumer.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public interface KafkaConsumerAssertNumberOfMessagesStep<K, V> {
    /**
     * Asserts that the consumer has received at least a specific number of messages.
     *
     * @param numberOfMessages the minimum number of messages expected
     * @return the next step in the assertion chain
     */
    KafkaConsumerAssertTimeoutStep<K, V> hasReceivedMessages(int numberOfMessages);

    /**
     * Asserts that the consumer has received messages within a specific timeout.
     *
     * @param timeout the maximum time to wait
     * @param timeUnit the unit of time for the timeout
     * @return the next step in the assertion chain
     */
    KafkaConsumerAssertWhereStep<K, V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit);

    /**
     * Asserts that the consumer has received a message with a value satisfying the given requirements.
     *
     * @param <T> the type of the value
     * @param type the class of the value type
     * @param requirements the consumer defining the requirements to be satisfied
     */
    default <T> void hasReceivedValueSatisfying(Class<T> type, Consumer<T> requirements) {
        hasReceivedMessagesWithin(5, TimeUnit.SECONDS)
                .where(records -> records.anySatisfy(record ->
                        assertThat(record.value()).isInstanceOfSatisfying(type, requirements)));
    }
}
