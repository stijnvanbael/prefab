package be.appify.prefab.test.kafka.asserts;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.ListAssert;

import java.util.function.Consumer;

public interface KafkaConsumerAssertWhereStep<K, V> {
    /// Asserts that the consumer has received messages that satisfy the given assertion. The `ListAssert` in the
    /// consumer asserts all received messages.
    void where(Consumer<ListAssert<ConsumerRecord<K, V>>> assertion);
}
