package be.appify.prefab.test.asserts;

import be.appify.prefab.test.EventConsumer;
import org.assertj.core.api.Assertions;

/**
 * Entry point for unified event consumer assertions.
 * <p>
 * Use this class to assert events received by an {@link EventConsumer}, regardless of the underlying
 * messaging system (Kafka, Pub/Sub, SNS/SQS).
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * EventAssertions.assertThat(userConsumer)
 *     .hasReceivedValueSatisfying(UserEvent.Created.class, event -> {
 *         assertThat(event.name()).isEqualTo("Alice");
 *     });
 * }
 * </pre>
 */
public class EventAssertions extends Assertions {

    private EventAssertions() {
    }

    /**
     * Creates a new {@link EventConsumerNumberOfMessagesStep} for the given {@link EventConsumer}.
     *
     * @param consumer the consumer to create assertions for
     * @param <V>      the type of events the consumer receives
     * @return a new {@link EventConsumerNumberOfMessagesStep}
     */
    public static <V> EventConsumerNumberOfMessagesStep<V> assertThat(EventConsumer<V> consumer) {
        return EventConsumerAssert.assertThat(consumer);
    }
}
