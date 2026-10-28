package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.StreamProcessorContext;
import java.util.Optional;

/** Read-only view on a Kafka Streams global store. */
final class KafkaPrefabGlobalStoreAdapter<K, V> implements GlobalStore<K, V> {
    private final KafkaPrefabStoreAdapter<K, V> delegate;

    KafkaPrefabGlobalStoreAdapter(String name) {
        this.delegate = new KafkaPrefabStoreAdapter<>(name);
    }

    @Override
    public Optional<V> get(K key) {
        return delegate.get(key);
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public void init(StreamProcessorContext<?, ?> context) {
        delegate.init(context);
    }
}
