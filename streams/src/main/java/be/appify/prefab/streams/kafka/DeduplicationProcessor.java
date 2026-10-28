package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import java.time.Duration;
import java.util.function.Function;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.WindowStore;

/**
 * Forwards a record only when its event identity was not seen within the retention period around its event time.
 */
final class DeduplicationProcessor<K, V extends Keyed<K>> extends ContextualProcessor<K, V, K, V> {
    private final String storeName;
    private final Function<V, String> idExtractor;
    private final long retentionMillis;
    private WindowStore<String, Long> store;

    DeduplicationProcessor(String storeName, Function<V, String> idExtractor, Duration retention) {
        this.storeName = storeName;
        this.idExtractor = idExtractor;
        this.retentionMillis = retention.toMillis();
    }

    @Override
    public void init(org.apache.kafka.streams.processor.api.ProcessorContext<K, V> context) {
        super.init(context);
        store = context.getStateStore(storeName);
    }

    @Override
    public void process(Record<K, V> record) {
        var id = record.value() == null ? null : idExtractor.apply(record.value());
        if (id == null) {
            context().forward(record);
            return;
        }
        var eventTime = record.timestamp();
        if (isDuplicate(id, eventTime)) {
            return;
        }
        store.put(id, eventTime, eventTime);
        context().forward(record);
    }

    private boolean isDuplicate(String id, long eventTime) {
        try (var seen = store.fetch(id, eventTime - retentionMillis, eventTime + retentionMillis)) {
            return seen.hasNext();
        }
    }
}
