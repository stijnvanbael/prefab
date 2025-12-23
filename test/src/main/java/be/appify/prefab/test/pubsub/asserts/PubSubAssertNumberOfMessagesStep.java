package be.appify.prefab.test.pubsub.asserts;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public interface PubSubAssertNumberOfMessagesStep<V> {
    /// Asserts that the subscriber has received at least a specific number of messages.
    PubSubSubscriberAssertTimeoutStep<V> hasReceivedMessages(int numberOfMessages);

    /// Asserts that the subscriber has received messages within a specific timeout.
    PubSubSubscriberAssertWhereStep<V> hasReceivedMessagesWithin(int timeout, TimeUnit timeUnit);

    /// Asserts that the subscriber has received a message satisfying the given requirements.
    default <T> void hasReceivedValueSatisfying(Class<T> type, Consumer<T> requirements) {
        hasReceivedMessagesWithin(5, TimeUnit.SECONDS)
                .where(records -> records.anySatisfy(record ->
                        assertThat(record).isInstanceOfSatisfying(type, requirements)));
    }
}
