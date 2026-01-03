package be.appify.prefab.test.pubsub.asserts;

import be.appify.prefab.test.pubsub.Subscriber;
import org.assertj.core.api.Assertions;

/**
 * Entry point for PubSub related assertions.
 */
public class PubSubAssertions extends Assertions {

    private PubSubAssertions() {
    }

    /**
     * Creates a new {@link PubSubAssertNumberOfMessagesStep} for the given {@link Subscriber}.
     *
     * @param subscriber the subscriber to create assertions for
     * @param <V>        the type of messages the subscriber receives
     * @return a new {@link PubSubAssertNumberOfMessagesStep}
     */
    public static <V> PubSubAssertNumberOfMessagesStep<V> assertThat(Subscriber<V> subscriber) {
        return PubSubSubscriberAssert.assertThat(subscriber);
    }
}
