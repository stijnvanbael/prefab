package be.appify.prefab.test.kafka.asserts;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.ListAssert;

import java.util.function.Consumer;

/**
 * Step for asserting messages received by a Kafka consumer based on custom conditions.
 *
 * @param <K>
 *         the type of the key
 * @param <V>
 *         the type of the value
 */
public interface KafkaConsumerAssertWhereStep<K, V> {
    /**
     * Asserts that the consumer has received messages that satisfy the given assertion. The <code>ListAssert</code> in
     * the consumer asserts all received messages.
     *
     * @param assertion
     *         the consumer defining the assertion to be satisfied
     */
    void where(Consumer<ListAssert<ConsumerRecord<K, V>>> assertion);
}
