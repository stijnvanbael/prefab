package be.appify.prefab.test.asserts;

import org.assertj.core.api.ListAssert;

import java.util.function.Consumer;

/**
 * Asserts events received by the consumer.
 *
 * @param <V> the type of events
 */
public interface EventConsumerWhereStep<V> {
    /**
     * Applies the given assertion on the list of received events.
     *
     * @param assertion the assertion to apply
     */
    void where(Consumer<ListAssert<V>> assertion);
}
