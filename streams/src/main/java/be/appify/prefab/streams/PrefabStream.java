package be.appify.prefab.streams;

import org.apache.kafka.streams.Topology;

/** Baseline source/sink stream definition contract. */
public interface PrefabStream {
    /**
     * Writes stream values to the topic registered for the provided event type.
     *
     * @param type
     *         event class registered for exactly one Kafka topic
     * @return current topology definition
     */
    Topology to(Class<?> type);

    /**
     * Writes stream values to an explicit topic name.
     *
     * @param topic
     *         Kafka topic
     * @return current topology definition
     */
    Topology to(String topic);
}

