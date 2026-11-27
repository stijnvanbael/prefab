package be.appify.prefab.example.showcase.capability;

/**
 * A showcase feature that not every technology variant supports.
 *
 * <p>Each variant lists the capabilities it lacks in {@code showcase-capabilities.yml}. Tests that need a capability
 * declare it with {@link RequiresCapability}; the coverage matrix in {@code examples/README.md} is derived from the same
 * files. Extend this enum when a showcase feature lands that may not work on every stack.</p>
 */
public enum Capability {
    /** Full-text filters on list endpoints. */
    FULL_TEXT_SEARCH,
    /** Range filters (from/to) on list endpoints. */
    RANGE_FILTER,
    /** Aggregates modelled as sealed interfaces with a persisted type discriminator. */
    POLYMORPHIC_AGGREGATES,
    /** Binary content served through {@code @Download}. */
    BINARY_DOWNLOAD,
    /** Per-key ordering of events published with a partitioning key. */
    ORDERED_DELIVERY,
    /** Prefab Streams topologies. */
    STREAMS,
    /** Kafka-native Prefab Streams breakout operators. */
    KAFKA_STREAMS_BREAKOUT,
    /** Events serialized as Avro. */
    AVRO,
    /** Spring Security integration. */
    SECURITY,
    /** Generated OpenAPI documentation. */
    OPENAPI,
    /** Generated AsyncAPI documentation. */
    ASYNCAPI
}
