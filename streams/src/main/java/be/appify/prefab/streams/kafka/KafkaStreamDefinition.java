package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamDefinition;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.kafka.streams.Topology;

/** Kafka Streams implementation of {@link StreamDefinition}. */
public final class KafkaStreamDefinition implements StreamDefinition, KafkaTopologyElement {
    private final Supplier<Topology> topologySupplier;
    private volatile Topology nativeTopology;

    /**
     * Constructs a new KafkaStreamDefinition.
     *
     * @param topologySupplier supplies the native topology on demand
     */
    public KafkaStreamDefinition(Supplier<Topology> topologySupplier) {
        this.topologySupplier = Objects.requireNonNull(topologySupplier, "topologySupplier must not be null");
    }

    /**
     * Builds the native Kafka topology on demand and caches the result.
     *
     * @return the native topology
     */
    public Topology buildTopology() {
        var cachedTopology = nativeTopology;
        if (cachedTopology == null) {
            synchronized (this) {
                cachedTopology = nativeTopology;
                if (cachedTopology == null) {
                    cachedTopology = Objects.requireNonNull(topologySupplier.get(), "topologySupplier must not return null");
                    nativeTopology = cachedTopology;
                }
            }
        }
        return cachedTopology;
    }

    @Override
    public Topology nativeTopology() {
        return buildTopology();
    }
}
