package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Keyed;

import java.time.Duration;
import java.util.List;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface PrefabStream<K, V extends Keyed<K>> {

    PrefabStream<K, V> filter(Predicate<V> predicate);

    <VO extends Keyed<K>> PrefabStream<K, VO> map(Function<V, VO> mapper);

    <VO extends Keyed<K>> PrefabStream<K, VO> flatMap(Function<V, Iterable<VO>> mapper);

    PrefabStream<K, V> branch(Predicate<V> predicate);

    <S extends V> PrefabStream<K, S> branch(Class<S> subtype);

    PrefabStream<K, V> merge(PrefabStream<K, V> other);

    <VO extends Keyed<K>, VR extends Keyed<K>> PrefabStream<K, VR> join(
            PrefabStream<K, VO> other,
            JoinWindow window,
            BiFunction<? super V, ? super VO, ? extends VR> joiner
    );

    /**
     * Groups the records by a key derived from the value, re-partitioning when the key differs.
     *
     * @param keyType
     *         the type of the group key
     * @param keyExtractor
     *         derives the group key from a value
     * @param <KO>
     *         the group key type
     * @return a grouped stream on which a non-windowed aggregation can be defined
     */
    <KO> PrefabGroupedStream<KO, V> groupBy(Class<KO> keyType, Function<V, KO> keyExtractor);

    /**
     * Re-keys the stream: every value is mapped to a value whose {@link Keyed#key()} becomes the new record key,
     * and the records are re-partitioned on a generated repartition topic name.
     *
     * @param keyType
     *         the type of the new key
     * @param mapper
     *         maps a value to the re-keyed value
     * @param <KO>
     *         the new key type
     * @param <VO>
     *         the re-keyed value type
     * @return the re-keyed stream
     */
    <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> keyBy(Class<KO> keyType, Function<V, VO> mapper);

    /**
     * Re-keys the stream like {@link #keyBy(Class, Function)}, using an explicit repartition topic name.
     *
     * @param keyType
     *         the type of the new key
     * @param mapper
     *         maps a value to the re-keyed value
     * @param repartitionName
     *         the user-chosen name of the repartition topic, see {@link #repartition(String)}
     * @param <KO>
     *         the new key type
     * @param <VO>
     *         the re-keyed value type
     * @return the re-keyed stream
     * @throws IllegalArgumentException
     *         if the name is invalid or already used in the topology
     */
    <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> keyBy(
            Class<KO> keyType,
            Function<V, VO> mapper,
            String repartitionName
    );

    /**
     * Redistributes the records over the partitions of a repartition topic with a generated name, keeping the key.
     *
     * @return the repartitioned stream
     */
    PrefabStream<K, V> repartition();

    /**
     * Redistributes the records over the partitions of a repartition topic with an explicit name, keeping the key.
     *
     * <p>The name is used as the repartition topic name in the topology. On Kafka the application id is prefixed and
     * {@code -repartition} is appended by Kafka Streams. The name may contain only letters, digits, {@code .},
     * {@code _} and {@code -}, and must be unique within the topology.
     *
     * @param name
     *         the repartition topic name
     * @return the repartitioned stream
     * @throws IllegalArgumentException
     *         if the name is invalid or already used in the topology
     */
    PrefabStream<K, V> repartition(String name);

    /**
     * Suppresses duplicate records, using the string form of the value's {@link Keyed#key()} as event identity.
     *
     * @param retention
     *         how long an event identity is remembered, measured in event time
     * @return the stream without duplicates
     * @see #deduplicate(Function, Duration)
     */
    default PrefabStream<K, V> deduplicate(Duration retention) {
        return deduplicate(value -> String.valueOf(value.key()), retention);
    }

    /**
     * Suppresses records whose event identity was already seen within the retention period.
     *
     * <p>The first record per identity is forwarded unchanged, later records with the same identity whose event
     * time lies within {@code retention} of an earlier one are dropped. Records for which the extractor returns
     * {@code null} are always forwarded. Identities are tracked per partition, so all records sharing an identity
     * must share a record key (re-key with {@link #keyBy(Class, Function)} if needed).
     *
     * @param idExtractor
     *         derives the event identity from a value
     * @param retention
     *         how long an event identity is remembered, measured in event time; must be positive
     * @return the stream without duplicates
     */
    PrefabStream<K, V> deduplicate(Function<V, String> idExtractor, Duration retention);
    <NI, NO, KO, VO extends Keyed<KO>> PrefabStream<KO, VO> breakout(
            StreamBreakoutAdapter<K, V, KO, VO, NI, NO> adapter
    );

    <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> process(
            StreamProcessor<K, V, KO, VO> processor
    );

    default <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> process(
            StreamProcessor<K, V, KO, VO> processor,
            Store<?, ?>... stores
    ) {
        return process(new StoreBindingStreamProcessor<>(processor, Arrays.asList(stores)));
    }

    StreamDefinition to(Class<? super V> type);

    StreamDefinition to(String topic);

    PrefabStream<K, V> unwrap();

    /**
     * Returns the concrete runtime class of the stream's value type {@code V}, when it is known.
     *
     * <p>Concrete backend implementations override this to supply the class; decorators delegate
     * to their wrapped stream. The default returns {@code null} (unknown).
     *
     * <p>This is used by {@link #aggregate} to produce a human-readable, unique state-store name
     * that encodes the input value type instead of the raw generic type-variable names.
     */
    default Class<V> knownValueType() {
        return null;
    }

    default <KO, VO extends Keyed<KO>> PrefabStream<KO, VO> aggregate(
            Function<V, KO> groupBy,
            Function<List<V>, VO> aggregation,
            Predicate<VO> egressCondition
    ) {
        return process(new AggregationProcessor<>(groupBy, aggregation, egressCondition, knownValueType()));
    }
}
