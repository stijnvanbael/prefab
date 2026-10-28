package be.appify.prefab.streams.kafka;

import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

/** Applies every record of a global store's source topic to the store; a {@code null} value deletes the key. */
final class GlobalStoreUpdateProcessor<K, V> extends ContextualProcessor<K, V, Void, Void> {
    private final String storeName;
    private KeyValueStore<K, V> store;

    GlobalStoreUpdateProcessor(String storeName) {
        this.storeName = storeName;
    }

    @Override
    public void init(ProcessorContext<Void, Void> context) {
        super.init(context);
        store = context.getStateStore(storeName);
    }

    @Override
    public void process(Record<K, V> record) {
        if (record.key() == null) {
            return;
        }
        if (record.value() == null) {
            store.delete(record.key());
        } else {
            store.put(record.key(), record.value());
        }
    }
}
