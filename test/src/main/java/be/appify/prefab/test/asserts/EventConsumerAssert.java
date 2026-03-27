package be.appify.prefab.test.asserts;

import be.appify.prefab.test.EventConsumer;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.awaitility.Awaitility;
import org.awaitility.core.DurationFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Asserts events received by the unified {@link EventConsumer}.
 * <p>
 * For example:
 * <pre>
 * {@code
 * EventConsumerAssert.assertThat(consumer)
 *    .hasReceivedMessages(3)
 *    .within(5, TimeUnit.SECONDS)
 *    .where(events -> events.containsExactlyInAnyOrder(expectedValues));
 * }
 * </pre>
 *
 * @param <V> the type of events
 */
public final class EventConsumerAssert<V> implements
        EventConsumerNumberOfMessagesStep<V>,
        EventConsumerTimeoutStep<V>,
        EventConsumerWhereStep<V> {
    private final EventConsumer<V> consumer;
    private int numberOfMessages;
    private Duration timeout;

    /**
     * Creates a new {@link EventConsumerAssert} for the given consumer.
     *
     * @param consumer the consumer to assert on
     */
    public EventConsumerAssert(EventConsumer<V> consumer) {
        this.consumer = consumer;
    }

    /**
     * Creates a new {@link EventConsumerAssert} for the given consumer.
     *
     * @param consumer the consumer to assert on
     * @param <V>      the type of events
     * @return a new {@link EventConsumerNumberOfMessagesStep}
     */
    public static <V> EventConsumerNumberOfMessagesStep<V> assertThat(EventConsumer<V> consumer) {
        return new EventConsumerAssert<>(consumer);
    }

    @Override
    public EventConsumerTimeoutStep<V> hasReceivedMessages(int numberOfMessages) {
        this.numberOfMessages = numberOfMessages;
        return this;
    }

    @Override
    public EventConsumerWhereStep<V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit) {
        this.timeout = DurationFactory.of(timeout, timeUnit);
        return this;
    }

    @Override
    public EventConsumerWhereStep<V> within(long timeout, TimeUnit timeUnit) {
        this.timeout = DurationFactory.of(timeout, timeUnit);
        return this;
    }

    @Override
    public void where(Consumer<ListAssert<V>> assertion) {
        Awaitility.await().atMost(timeout).untilAsserted(() -> {
            var records = numberOfMessages != 0
                    ? consumer.messages().stream().limit(numberOfMessages).toList()
                    : consumer.messages();
            if (numberOfMessages > 0) {
                Assertions.assertThat(records).hasSizeGreaterThanOrEqualTo(numberOfMessages);
            } else {
                Assertions.assertThat(records).isNotEmpty();
            }
            assertion.accept(Assertions.assertThat(records));
        });
    }
}
