package be.appify.prefab.test.kafka.asserts;

import org.apache.kafka.clients.consumer.Consumer;
import org.assertj.core.api.Assertions;

/**
 * Entry point for Kafka assertions.
 */
public class KafkaAssertions extends Assertions {

    private KafkaAssertions() {
    }

    /**
     * Creates a new {@link KafkaConsumerAssertNumberOfMessagesStep} for the given Kafka consumer.
     *
     * @param consumer the Kafka consumer to assert on
     * @param <K>      the type of the key
     * @param <V>      the type of the value
     * @return a new {@link KafkaConsumerAssertNumberOfMessagesStep}
     */
    public static <K, V> KafkaConsumerAssertNumberOfMessagesStep<K, V> assertThat(Consumer<K, V> consumer) {
        return KafkaConsumerAssert.assertThat(consumer);
    }
}
