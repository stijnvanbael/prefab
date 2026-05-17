package be.appify.prefab.streams;

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
     * Routes records that match {@code predicate} to a new branch.
     *
     * <p>Records that do not match are dropped from the returned stream.
     *
     * @param predicate branch predicate; must not be {@code null}
     * @return stream containing only matching records
     */
    PrefabStream<V> branch(Predicate<V> predicate);

    /**
     * Routes only values assignable to {@code subtype} and casts them to that subtype.
     *
     * @param subtype subtype class to filter and cast to; must not be {@code null}
     * @param <S>     subtype of {@code V}
     * @return stream containing only values of {@code subtype}
     */
    <S extends V> PrefabStream<S> branch(Class<S> subtype);

    /**
     * Merges the current stream with another stream whose values are assignable to {@code V}.
     *
     * <p>For merges that should widen sibling streams into an explicit common supertype, use the
     * {@code PrefabStreams.merge(left, right)} factory API.
     *
     * @param other stream to merge with
     * @return merged stream containing records from both inputs as {@code V}
     */
    PrefabStream<V> merge(PrefabStream<? extends V> other);

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
