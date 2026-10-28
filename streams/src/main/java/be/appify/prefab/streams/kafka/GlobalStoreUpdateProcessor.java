package be.appify.prefab.streams.kafka;

import java.util.function.Function;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Applies every record of a global store's source topic to the store. A {@code null} source value, or a value that
 * maps to {@code null}, deletes the key.
 */
final class GlobalStoreUpdateProcessor<K, S, V> extends ContextualProcessor<K, S, Void, Void> {
    private final String storeName;
    private final Function<S, V> mapper;
    private KeyValueStore<K, V> store;

    GlobalStoreUpdateProcessor(String storeName, Function<S, V> mapper) {
        this.storeName = storeName;
        this.mapper = mapper;
    }

    @Override
    public void init(ProcessorContext<Void, Void> context) {
        super.init(context);
        store = context.getStateStore(storeName);
    }

    @Override
    public void process(Record<K, S> record) {
        if (record.key() == null) {
            return;
        }
        var value = record.value() == null ? null : mapper.apply(record.value());
        if (value == null) {
            store.delete(record.key());
        } else {
            store.put(record.key(), value);
        }
    }
}
