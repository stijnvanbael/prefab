package be.appify.prefab.test.sns.asserts;

import be.appify.prefab.test.sns.SqsSubscriber;
import org.assertj.core.api.Assertions;

/**
 * Entry point for SQS related assertions.
 *
 * @deprecated Use {@link be.appify.prefab.test.asserts.EventAssertions} instead.
 */
@Deprecated
public class SqsAssertions extends Assertions {

    private SqsAssertions() {
    }

    /**
     * Creates a new {@link SqsAssertNumberOfMessagesStep} for the given {@link SqsSubscriber}.
     *
     * @param subscriber the subscriber to create assertions for
     * @param <V>        the type of messages the subscriber receives
     * @return a new {@link SqsAssertNumberOfMessagesStep}
     */
    public static <V> SqsAssertNumberOfMessagesStep<V> assertThat(SqsSubscriber<V> subscriber) {
        return SqsSubscriberAssert.assertThat(subscriber);
    }
}
