package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamBackend;

/** The Kafka Streams backend of Prefab Streams. */
public final class KafkaStreamBackend {
    /** Identifier of the Kafka Streams backend. */
    public static final StreamBackend KAFKA = StreamBackend.named("kafka");

    private KafkaStreamBackend() {
    }
}
