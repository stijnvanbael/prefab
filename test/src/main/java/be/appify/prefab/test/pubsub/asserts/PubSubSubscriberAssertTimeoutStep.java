package be.appify.prefab.test.pubsub.asserts;

import java.util.concurrent.TimeUnit;

public interface PubSubSubscriberAssertTimeoutStep<V> {
    /// Asserts that the subscriber has received messages within a specific timeout.
    PubSubSubscriberAssertWhereStep<V> within(long timeout, TimeUnit timeUnit);
}
