package be.appify.prefab.streams;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Fluent DSL builder for a single stream pipeline. Parameterised on the current value type {@code V}.
 *
 * <p>Each stateless operator returns a new {@code PrefabStream} with the transformed type, enabling
 * type-safe operator chaining. Terminal operations ({@code to}) materialise the pipeline definition.
 *
 * @param <V> current record value type
 */
public interface PrefabStream<V> {

    /**
     * Keeps only records for which {@code predicate} returns {@code true}.
     *
     * @param predicate test applied to each record value; must not be {@code null}
     * @return stream with non-matching records removed
     */
    PrefabStream<V> filter(Predicate<V> predicate);

    /**
     * Transforms each record value using {@code mapper}.
     *
     * @param mapper value transformation function; must not be {@code null}
     * @param <R>    output value type
     * @return stream of mapped values
     */
    <R> PrefabStream<R> map(Function<V, R> mapper);

    /**
     * Expands each record value into zero or more output values using {@code mapper}.
     *
     * @param mapper function returning an {@link Iterable} of output values; must not be {@code null}
     * @param <R>    output value type
     * @return stream of expanded values
     */
    <R> PrefabStream<R> flatMap(Function<V, Iterable<R>> mapper);

    /**
     * Splits the stream into ordered branches using first-match semantics.
     *
     * <p>Each record is routed to the first predicate that matches. Records that do not match any
     * predicate are dropped.
     *
     * @param predicates branch predicates in evaluation order; must not be {@code null}
     * @return one stream per predicate, preserving predicate order
     */
    List<PrefabStream<V>> branch(Predicate<V>... predicates);

    /**
     * Merges the current stream with another stream of the same value type.
     *
     * @param other stream to merge with
     * @return merged stream containing records from both inputs
     */
    PrefabStream<V> merge(PrefabStream<V> other);

    /**
     * Injects a backend-native stream fragment using a backend adapter SPI.
     *
     * @param adapter breakout adapter bound to a concrete backend
     * @param <R> resulting value type after breakout
     * @param <NATIVE_IN> backend-native input stream type
     * @param <NATIVE_OUT> backend-native output stream type
     * @return stream wrapping the adapted native fragment
     */
    <R, NATIVE_IN, NATIVE_OUT> PrefabStream<R> breakout(
            StreamBreakoutAdapter<V, R, NATIVE_IN, NATIVE_OUT> adapter
    );

    /**
     * Writes stream values to the topic registered for the provided event type.
     *
     * @param type event class registered for exactly one Kafka topic
     * @return current stream definition wrapper
     */
    StreamDefinition to(Class<?> type);

    /**
     * Writes stream values to an explicit topic name.
     *
     * @param topic Kafka topic
     * @return current stream definition wrapper
     */
    StreamDefinition to(String topic);
}
