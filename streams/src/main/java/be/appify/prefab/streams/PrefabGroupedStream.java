package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A stream whose records have been grouped by a key, ready for a stateful, non-windowed aggregation.
 *
 * @param <KG>
 *         the group key type
 * @param <V>
 *         the input value type
 */
public interface PrefabGroupedStream<KG, V> {

    /**
     * Continuously aggregates the values of each group into a running result.
     *
     * <p>For every incoming record the aggregator is applied to the record and the current state of its
     * group (or a fresh state from the initializer for a new group); the updated state is stored and emitted
     * downstream, keyed by the group key. Records with a {@code null} value are ignored.
     *
     * @param resultType
     *         the runtime type of the aggregate, used for serialization and naming of the state store
     * @param initializer
     *         creates the initial state of a group
     * @param aggregator
     *         combines an incoming value with the current state of its group
     * @param <VR>
     *         the aggregate type
     * @return a stream emitting the updated aggregate after each incoming record
     */
    <VR extends Keyed<KG>> PrefabStream<KG, VR> aggregate(
            Class<VR> resultType,
            Supplier<VR> initializer,
            BiFunction<? super V, ? super VR, ? extends VR> aggregator
    );
}
