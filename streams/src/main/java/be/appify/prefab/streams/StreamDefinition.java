package be.appify.prefab.streams;

import java.util.Objects;
import java.util.function.Supplier;
import org.apache.kafka.streams.Topology;

/** Prefab wrapper around a backend-native stream topology definition. */
public final class StreamDefinition {
    private final Supplier<Topology> topologySupplier;
    private volatile Topology nativeTopology;

    /** Constructs a new StreamDefinition. */
    public StreamDefinition(Supplier<Topology> topologySupplier) {
        this.topologySupplier = Objects.requireNonNull(topologySupplier, "topologySupplier must not be null");
    }

    /** Builds the native Kafka topology on demand and caches the result. */
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

    /** Returns the native Kafka topology for runtime wiring and tests. */
    public Topology nativeTopology() {
        return buildTopology();
    }
}

