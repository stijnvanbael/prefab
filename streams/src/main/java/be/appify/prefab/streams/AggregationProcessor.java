package be.appify.prefab.streams;

import be.appify.prefab.core.domain.Key;
import be.appify.prefab.core.domain.Keyed;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class AggregationProcessor<K extends Key<K>, V extends Keyed<K>, KO extends Key<KO>, VO extends Keyed<KO>>
        extends StatefulStreamProcessor<K, V, KO, VO> {
    private final Function<V, KO> groupBy;
    private final Function<List<V>, VO> aggregator;
    private final Predicate<VO> egressCondition;

    public AggregationProcessor(
            Function<V, KO> groupBy,
            Function<List<V>, VO> aggregator,
            Predicate<VO> egressCondition
    ) {
        super(new TypeReference<Aggregation<KO, V>>() {
        });
        this.groupBy = groupBy;
        this.aggregator = aggregator;
        this.egressCondition = egressCondition;
    }

    @Override
    public void process(StreamRecord<K, V> streamRecord) {
        var store = store(new TypeReference<Aggregation<KO, V>>() {
        });
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
}
