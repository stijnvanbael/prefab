package be.appify.prefab.test.pubsub.asserts;

import be.appify.prefab.test.pubsub.Subscriber;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.awaitility.Awaitility;
import org.awaitility.core.DurationFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Asserts messages received by the subscriber.
 * <p>
 * For example:
 * <pre>
 * {@code
 * PubSubSubscriberAssert.assertThat(subscriber)
 *    .hasReceivedMessages(3)
 *    .within(5, TimeUnit.SECONDS)
 *    .where(records -> records.containsExactlyInAnyOrder(expectedValues));
 * }
 * </pre>
 *
 * @param <V> the type of messages
 */
public final class PubSubSubscriberAssert<V> implements
        PubSubAssertNumberOfMessagesStep<V>,
        PubSubSubscriberAssertTimeoutStep<V>,
        PubSubSubscriberAssertWhereStep<V> {
    private final Subscriber<V> subscriber;
    private int numberOfMessages;
    private Duration timeout;

    /**
     * Constructor for PubSubSubscriberAssert.
     *
     * @param subscriber the subscriber to assert on
     */
    public PubSubSubscriberAssert(Subscriber<V> subscriber) {
        this.subscriber = subscriber;
    }

    /**
     * Creates a new PubSubSubscriberAssert for the given subscriber.
     *
     * @param subscriber the subscriber to assert on
     * @param <V>        the type of messages
     * @return a new PubSubSubscriberAssert instance
     */
    public static <V> PubSubAssertNumberOfMessagesStep<V> assertThat(Subscriber<V> subscriber) {
        return new PubSubSubscriberAssert<>(subscriber);
    }

    @Override
    public PubSubSubscriberAssertTimeoutStep<V> hasReceivedMessages(int numberOfMessages) {
        this.numberOfMessages = numberOfMessages;
        return this;
    }

    @Override
    public PubSubSubscriberAssertWhereStep<V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit) {
        this.timeout = DurationFactory.of(timeout, timeUnit);
        return this;
    }

    @Override
    public PubSubSubscriberAssertWhereStep<V> within(long timeout, TimeUnit timeUnit) {
        this.timeout = DurationFactory.of(timeout, timeUnit);
        return this;
    }

    @Override
    public void where(Consumer<ListAssert<V>> assertion) {
        Awaitility.await().atMost(timeout).untilAsserted(() -> {
           var records = numberOfMessages != 0
                   ? subscriber.messages().stream().limit(numberOfMessages).toList()
                   : subscriber.messages();
           if(numberOfMessages > 0) {
               Assertions.assertThat(records).hasSizeGreaterThanOrEqualTo(numberOfMessages);
           } else {
               Assertions.assertThat(records).isNotEmpty();
           }
        });
    }
}
