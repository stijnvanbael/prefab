package be.appify.prefab.test.sns.asserts;

import be.appify.prefab.test.sns.SqsSubscriber;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.awaitility.Awaitility;
import org.awaitility.core.DurationFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Asserts messages received by the SQS subscriber.
 * <p>
 * For example:
 * <pre>
 * {@code
 * SqsSubscriberAssert.assertThat(subscriber)
 *    .hasReceivedMessages(3)
 *    .within(5, TimeUnit.SECONDS)
 *    .where(records -> records.containsExactlyInAnyOrder(expectedValues));
 * }
 * </pre>
 *
 * @param <V> the type of messages
 */
public final class SqsSubscriberAssert<V> implements
        SqsAssertNumberOfMessagesStep<V>,
        SqsSubscriberAssertTimeoutStep<V>,
        SqsSubscriberAssertWhereStep<V> {
    private final SqsSubscriber<V> subscriber;
    private int numberOfMessages;
    private Duration timeout;

    /**
     * Constructor for SqsSubscriberAssert.
     *
     * @param subscriber the subscriber to assert on
     */
    public SqsSubscriberAssert(SqsSubscriber<V> subscriber) {
        this.subscriber = subscriber;
    }

    /**
     * Creates a new SqsSubscriberAssert for the given subscriber.
     *
     * @param subscriber the subscriber to assert on
     * @param <V>        the type of messages
     * @return a new SqsSubscriberAssert instance
     */
    public static <V> SqsAssertNumberOfMessagesStep<V> assertThat(SqsSubscriber<V> subscriber) {
        return new SqsSubscriberAssert<>(subscriber);
    }

    @Override
    public SqsSubscriberAssertTimeoutStep<V> hasReceivedMessages(int numberOfMessages) {
        this.numberOfMessages = numberOfMessages;
        return this;
    }

    @Override
    public SqsSubscriberAssertWhereStep<V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit) {
        this.timeout = DurationFactory.of(timeout, timeUnit);
        return this;
    }

    @Override
    public SqsSubscriberAssertWhereStep<V> within(long timeout, TimeUnit timeUnit) {
        this.timeout = DurationFactory.of(timeout, timeUnit);
        return this;
    }

    @Override
    public void where(Consumer<ListAssert<V>> assertion) {
        Awaitility.await().atMost(timeout).untilAsserted(() -> {
            var records = numberOfMessages != 0
                    ? subscriber.messages().stream().limit(numberOfMessages).toList()
                    : subscriber.messages();
            if (numberOfMessages > 0) {
                Assertions.assertThat(records).hasSizeGreaterThanOrEqualTo(numberOfMessages);
            } else {
                Assertions.assertThat(records).isNotEmpty();
            }
            assertion.accept(Assertions.assertThat(records));
        });
    }
}
