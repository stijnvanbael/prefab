package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.StreamProcessorContext;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueStore;

/** Read-only view on a Kafka Streams global store. */
final class KafkaPrefabGlobalStoreAdapter<K, V> implements GlobalStore<K, V>, KafkaTopologyElement, KafkaStateStoreAccess {
    private final KafkaPrefabStoreAdapter<K, V> delegate;
    private final KafkaStreamDefinition topology;

    KafkaPrefabGlobalStoreAdapter(String name, Supplier<Topology> topologySupplier) {
        this.delegate = new KafkaPrefabStoreAdapter<>(name);
        this.topology = new KafkaStreamDefinition(topologySupplier);
    }

    @Override
    public Optional<V> get(K key) {
        return delegate.get(key);
    }

    @Override
    public Topology nativeTopology() {
        return topology.nativeTopology();
    }

    @Override
    public String storeName() {
        return delegate.name();
    }

    @Override
    public void attach(KeyValueStore<?, ?> store) {
        delegate.attach(store);
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

