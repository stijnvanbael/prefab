package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class AggregationProcessor<K extends Key<K>, V extends Keyed<K>, KO extends Key<KO>, VO extends Keyed<KO>>
        extends StatefulStreamProcessor<K, V, KO, VO> {

    private final TypeReference<Aggregation<KO, V>> storeType;
    private final Function<V, KO> groupBy;
    private final Function<List<V>, VO> aggregator;
    private final Predicate<VO> egressCondition;

    /**
     * Constructs an {@code AggregationProcessor} without a known input value class. The state
     * store will receive a generic name derived from the raw {@code Aggregation} class.
     *
     * <p>Prefer {@link #AggregationProcessor(Function, Function, Predicate, Class)} when the
     * concrete input-value class is available (e.g. via {@link PrefabStream#knownValueType()});
     * that overload produces a more readable and unique store name.
     */
    public AggregationProcessor(
            Function<V, KO> groupBy,
            Function<List<V>, VO> aggregator,
            Predicate<VO> egressCondition
    ) {
        this(groupBy, aggregator, egressCondition, null);
    }

    /**
     * Constructs an {@code AggregationProcessor} with the concrete input value class.
     *
     * <p>When {@code inputClass} is not {@code null} the state-store name encodes the simple class
     * name of {@code V} — e.g. {@code aggregation-incoming-order} — making it human-readable and
     * unique within topologies that aggregate different value types.
     *
     * @param inputClass the runtime class of the stream's value type {@code V}, or {@code null}
     *                   when unknown (falls back to the raw {@code Aggregation} class name)
     */
    public AggregationProcessor(
            Function<V, KO> groupBy,
            Function<List<V>, VO> aggregator,
            Predicate<VO> egressCondition,
            Class<V> inputClass
    ) {
        this(deriveStoreType(inputClass), groupBy, aggregator, egressCondition);
    }

    private AggregationProcessor(
            TypeReference<Aggregation<KO, V>> storeType,
            Function<V, KO> groupBy,
            Function<List<V>, VO> aggregator,
            Predicate<VO> egressCondition
    ) {
        super(storeType);
        this.storeType = storeType;
        this.groupBy = groupBy;
        this.aggregator = aggregator;
        this.egressCondition = egressCondition;
    }

    @Override
    public void process(StreamRecord<K, V> streamRecord) {
        var store = store(storeType);
        var value = streamRecord.value();
        var key = groupBy.apply(value);
        var aggregation = store.get(key)
                .map(a -> a.append(value))
                .orElseGet(() -> Aggregation.create(key, value));
        store.put(key, aggregation);
        var output = aggregator.apply(aggregation.values());
        if (egressCondition.test(output)) {
            forward(key, output);
        }
    }

    /**
     * Builds the {@link TypeReference} used to name the state store.
     *
     * <p>When the concrete input value class is known the name is
     * {@code Aggregation<SimpleValueName>}, which {@code toStoreName()} converts to
     * {@code aggregation-simple-value-name}. This makes the store name readable and unique for
     * topologies that aggregate different value types. Falls back to the fully-qualified raw class
     * name when no value class is available.
     *
     * <p>Note: the output key type {@code KO} is not included because Java's standard
     * {@code LambdaMetafactory} erases the {@code groupBy} function's return type to
     * {@code Object} at runtime, making it impossible to extract {@code KO} from the lambda
     * without either serialisable lambdas or an explicit parameter. A future overload of
     * {@link PrefabStream#aggregate} that accepts {@code Class<KO>} explicitly is tracked
     * separately.
     */
    @SuppressWarnings("unchecked")
    private static <KO extends Key<KO>, V> TypeReference<Aggregation<KO, V>> deriveStoreType(
            Class<V> inputClass
    ) {
        var aggregationClass = (Class<Aggregation<KO, V>>) (Class<?>) Aggregation.class;
        if (inputClass != null) {
            var name = Aggregation.class.getSimpleName() + "<" + inputClass.getSimpleName() + ">";
            return TypeReference.of(aggregationClass, name);
        }
        return TypeReference.of(aggregationClass);
    }
}



