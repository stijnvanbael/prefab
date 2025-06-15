package be.appify.prefab.test.kafka.asserts;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.awaitility.Awaitility;
import org.awaitility.core.DurationFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

/// Asserts messages received by the consumer.
///
/// For example:
/// ```java
/// KafkaConsumerAssert.assertThat(consumer)
///    .hasReceivedMessages(3)
///    .within(5, TimeUnit.SECONDS)
///    .where(records -> records.extracting(ConsumerRecord::value)
///       .containsExactlyInAnyOrder(expectedValues));
///```
public final class KafkaConsumerAssert<K, V> implements
    KafkaConsumerAssertNumberOfMessagesStep<K, V>,
    KafkaConsumerAssertTimeoutStep<K, V>,
    KafkaConsumerAssertWhereStep<K, V> {
    private final Consumer<K, V> consumer;
    private int numberOfMessages;
    private Duration timeout;

    private KafkaConsumerAssert(Consumer<K, V> consumer) {
        this.consumer = consumer;
    }

    /// Asserts messages received by the consumer.
    public static <K, V> KafkaConsumerAssertNumberOfMessagesStep<K, V> assertThat(Consumer<K, V> consumer) {
        return new KafkaConsumerAssert<>(consumer);
    }

    @Override
    public KafkaConsumerAssertTimeoutStep<K, V> hasReceivedMessages(int numberOfMessages) {
        this.numberOfMessages = numberOfMessages;
        return this;
    }

    @Override
    public KafkaConsumerAssertWhereStep<K, V> within(long timeout, TimeUnit timeUnit) {
        this.timeout = DurationFactory.of(timeout, timeUnit);
        return this;
    }

    @Override
    public void where(java.util.function.Consumer<ListAssert<ConsumerRecord<K, V>>> assertion) {
        var allRecords = new ArrayList<ConsumerRecord<K, V>>();
        Awaitility.await().atMost(timeout).untilAsserted(() -> {
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1), numberOfMessages);
            allRecords.addAll(StreamSupport.stream(records.spliterator(), false).toList());
            assertion.accept(Assertions.assertThat(allRecords).hasSize(numberOfMessages));
        });
    }

}
