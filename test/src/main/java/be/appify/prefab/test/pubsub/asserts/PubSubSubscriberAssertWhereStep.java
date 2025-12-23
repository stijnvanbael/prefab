package be.appify.prefab.test.pubsub.asserts;

import org.assertj.core.api.ListAssert;

import java.util.function.Consumer;

public interface PubSubSubscriberAssertWhereStep<V> {
    /// Asserts that the subscriber has received messages that satisfy the given assertion. The `ListAssert` in the
    /// consumer asserts all received messages.
    void where(Consumer<ListAssert<V>> assertion);
}
