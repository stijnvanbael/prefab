package be.appify.prefab.test.pubsub.asserts;

import be.appify.prefab.test.pubsub.Subscriber;
import org.assertj.core.api.Assertions;

public class PubSubAssertions extends Assertions {
    public static <V> PubSubAssertNumberOfMessagesStep<V> assertThat(Subscriber<V> subscriber) {
        return PubSubSubscriberAssert.assertThat(subscriber);
    }
}
