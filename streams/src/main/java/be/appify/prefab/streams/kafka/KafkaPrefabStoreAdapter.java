package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamProcessorContext;
import java.util.Optional;
import org.apache.kafka.streams.state.KeyValueStore;

public class KafkaPrefabStoreAdapter<V> implements Store<V> {
    private final String name;
    private final ThreadLocal<KeyValueStore<String, V>> store = new ThreadLocal<>();

    public KafkaPrefabStoreAdapter(String name) {
        this.name = name;
    }

    @Override
    public Optional<V> get(String key) {
        return Optional.ofNullable(store().get(key));
    }

    @Override
    public void put(String key, V value) {
        store().put(key, value);
    }

    private KeyValueStore<String, V> store() {
        var store = this.store.get();
        if (store == null) {
            throw new IllegalStateException("Store is not initialized, call init() first");
        }
        return store;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void init(StreamProcessorContext<?> context) {
        store.set(((KafkaPrefabProcessorContext<?>) context).kafkaContext().getStateStore(name));
    }
}
