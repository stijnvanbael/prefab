package be.appify.prefab.test.pubsub.asserts;

import org.assertj.core.api.ListAssert;

import java.util.function.Consumer;

/**
 * Asserts messages received by the subscriber.
 *
 * @param <V> the type of messages
 */
public interface PubSubSubscriberAssertWhereStep<V> {
    /**
     * Applies the given assertion on the list of received messages.
     *
     * @param assertion the assertion to apply
     */
    void where(Consumer<ListAssert<V>> assertion);
}
