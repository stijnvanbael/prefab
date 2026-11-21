package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.GlobalStoreUpdater;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamProcessorContext;
import java.util.Optional;
import org.apache.kafka.streams.processor.api.ContextualProcessor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;

/**
 * Hands every record of a global store's source topic to a {@link GlobalStoreUpdater}. Kafka also replays the source
 * topic through this processor when the store is restored.
 */
final class GlobalStoreWriterProcessor<KI, VI extends Keyed<KI>, K, V extends Keyed<K>>
        extends ContextualProcessor<KI, VI, Void, Void> {
    private final String storeName;
    private final GlobalStoreUpdater<KI, VI, K, V> updater;
    private Store<K, V> store;

    GlobalStoreWriterProcessor(String storeName, GlobalStoreUpdater<KI, VI, K, V> updater) {
        this.storeName = storeName;
        this.updater = updater;
    }

    @Override
    public void init(ProcessorContext<Void, Void> context) {
        super.init(context);
        KeyValueStore<K, V> keyValueStore = context.getStateStore(storeName);
        store = new WritableStore<>(storeName, keyValueStore);
    }

    @Override
    public void process(Record<KI, VI> record) {
        if (record.key() == null || record.value() == null) {
            return;
        }
        updater.update(KafkaRecords.toStreamRecord(record), store);
    }

    private record WritableStore<K, V>(String name, KeyValueStore<K, V> delegate) implements Store<K, V> {
        @Override
        public Optional<V> get(K key) {
            return Optional.ofNullable(delegate.get(key));
        }

        @Override
        public void put(K key, V value) {
            delegate.put(key, value);
        }

        @Override
        public void init(StreamProcessorContext<?, ?> context) {
        }
    }
}
