package be.appify.prefab.test.asserts;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entry point for unified event consumer assertions.
 *
 * @param <V> the type of events the consumer receives
 */
public interface EventConsumerNumberOfMessagesStep<V> {
    /**
     * Asserts that the consumer has received at least the given number of events.
     *
     * @param numberOfMessages the minimum number of events expected
     * @return the next step in the assertion chain
     */
    EventConsumerTimeoutStep<V> hasReceivedMessages(int numberOfMessages);

    /**
     * Asserts that the consumer has received events within the given timeout.
     *
     * @param timeout  the timeout duration
     * @param timeUnit the time unit of the timeout
     * @return the next step in the assertion chain
     */
    EventConsumerWhereStep<V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit);

    /**
     * Asserts that the consumer has received an event with a value satisfying the given requirements.
     * Waits up to 5 seconds for the event to arrive.
     *
     * @param <T>          the type of the event
     * @param type         the class of the event type
     * @param requirements the consumer defining the requirements to be satisfied
     */
    default <T> void hasReceivedValueSatisfying(Class<T> type, Consumer<T> requirements) {
        hasReceivedMessagesWithin(5, TimeUnit.SECONDS)
                .where(records -> records.anySatisfy(record ->
                        assertThat(record).isInstanceOfSatisfying(type, requirements)));
    }
}
