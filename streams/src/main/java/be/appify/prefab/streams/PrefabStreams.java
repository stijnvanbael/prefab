package be.appify.prefab.streams;

/** Entry point for the Prefab streams DSL. */
public interface PrefabStreams {
    /**
     * Starts a stream definition from the topic mapped to the provided event type.
     *
     * @param type
     *         event class registered for a Kafka topic
     * @return source stream definition
     */
    PrefabStream from(Class<?> type);
}

