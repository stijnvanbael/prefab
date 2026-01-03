package be.appify.prefab.test.pubsub.asserts;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entry point for PubSub related assertions.
 *
 * @param <V> the type of messages the subscriber receives
 */
public interface PubSubAssertNumberOfMessagesStep<V> {
    /**
     * Asserts that the subscriber has received the given number of messages.
     * @param numberOfMessages the number of messages expected to be received
     * @return the next step in the assertion chain
     */
    PubSubSubscriberAssertTimeoutStep<V> hasReceivedMessages(int numberOfMessages);

    /**
     * Asserts that the subscriber has received messages within the given timeout.
     *
     * @param timeout the timeout duration
     * @param timeUnit the time unit of the timeout
     * @return the next step in the assertion chain
     */
    PubSubSubscriberAssertWhereStep<V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit);

    /**
     * Asserts that the subscriber has received a message with a value satisfying the given requirements.
     *
     * @param <T> the type of the value
     * @param type the class of the value type
     * @param requirements the consumer defining the requirements to be satisfied
     */
    default <T> void hasReceivedValueSatisfying(Class<T> type, Consumer<T> requirements) {
        hasReceivedMessagesWithin(5, TimeUnit.SECONDS)
                .where(records -> records.anySatisfy(record ->
                        assertThat(record).isInstanceOfSatisfying(type, requirements)));
    }
}
