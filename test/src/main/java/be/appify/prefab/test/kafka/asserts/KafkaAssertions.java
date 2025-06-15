package be.appify.prefab.test.kafka.asserts;

import org.apache.kafka.clients.consumer.Consumer;
import org.assertj.core.api.Assertions;

public class KafkaAssertions extends Assertions {
    public static <K, V> KafkaConsumerAssertNumberOfMessagesStep<K, V> assertThat(Consumer<K, V> consumer) {
        return KafkaConsumerAssert.assertThat(consumer);
    }
}
