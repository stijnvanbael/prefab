package be.appify.prefab.test.sns.asserts;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entry point for SQS related assertions.
 *
 * @param <V> the type of messages the subscriber receives
 * @deprecated Use {@link be.appify.prefab.test.asserts.EventConsumerNumberOfMessagesStep} instead.
 */
@Deprecated
public interface SqsAssertNumberOfMessagesStep<V> {
    /**
     * Asserts that the subscriber has received the given number of messages.
     *
     * @param numberOfMessages the number of messages expected to be received
     * @return the next step in the assertion chain
     */
    SqsSubscriberAssertTimeoutStep<V> hasReceivedMessages(int numberOfMessages);

    /**
     * Asserts that the subscriber has received messages within the given timeout.
     *
     * @param timeout the timeout duration
     * @param timeUnit the time unit of the timeout
     * @return the next step in the assertion chain
     */
    SqsSubscriberAssertWhereStep<V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit);

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
