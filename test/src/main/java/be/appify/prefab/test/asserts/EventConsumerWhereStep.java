package be.appify.prefab.test.asserts;

import org.assertj.core.api.ListAssert;

import java.lang.reflect.Constructor;
import java.util.List;
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

    /**
     * Instantiates a custom AssertJ assertion class over the received event list and applies the given assertion.
     * <p>
     * The assertion class must have a constructor that accepts a {@link List} of events.
     * </p>
     *
     * @param assertClass the custom AssertJ assertion class to instantiate
     * @param assertion   the assertion to apply on the custom assert object
     * @param <A>         the type of the custom assertion class
     */
    default <A> void where(Class<A> assertClass, Consumer<A> assertion) {
        where(events -> {
            try {
                Constructor<A> constructor = assertClass.getDeclaredConstructor(List.class);
                constructor.setAccessible(true);
                assertion.accept(constructor.newInstance(events.actual()));
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(
                        "Failed to instantiate " + assertClass.getName()
                                + ". Ensure it has a constructor accepting List<V>.", e);
            }
        });
    }
}
