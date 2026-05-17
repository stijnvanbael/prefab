package be.appify.prefab.streams;

/** Baseline source/sink stream definition contract. */
public interface PrefabStream {
    /**
     * Writes stream values to the topic registered for the provided event type.
     *
     * @param type
     *         event class registered for exactly one Kafka topic
     */
    void to(Class<?> type);

    /**
     * Writes stream values to an explicit topic name.
     *
     * @param topic
     *         Kafka topic
     */
    void to(String topic);
}

