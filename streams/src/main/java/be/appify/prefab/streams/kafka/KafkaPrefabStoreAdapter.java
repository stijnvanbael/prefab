package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.Store;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.Optional;

public class KafkaPrefabStoreAdapter<V> implements Store<V> {
    private final String name;
    private final KeyValueStore<String, V> store;

    public KafkaPrefabStoreAdapter(String name, KafkaPrefabProcessorContext<?> context) {
        this.name = name;
        store = context.kafkaContext().getStateStore(name);
    }

    @Override
    public Optional<V> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void put(String key, V value) {
        store.put(key, value);
    }

    @Override
    public String name() {
        return name;
    }
}
