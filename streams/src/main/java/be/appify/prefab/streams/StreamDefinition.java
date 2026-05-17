package be.appify.prefab.streams;

import org.apache.kafka.streams.Topology;

/** Prefab wrapper around a backend-native stream topology definition. */
public final class StreamDefinition {
    private final Topology nativeTopology;

    /** Constructs a new StreamDefinition. */
    public StreamDefinition(Topology nativeTopology) {
        this.nativeTopology = java.util.Objects.requireNonNull(nativeTopology, "nativeTopology must not be null");
    }

    /** Returns the native Kafka topology for runtime wiring and tests. */
    public Topology nativeTopology() {
        return nativeTopology;
    }
}

