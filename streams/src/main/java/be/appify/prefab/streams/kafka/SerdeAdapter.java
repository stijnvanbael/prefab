package be.appify.prefab.streams.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Lightweight serde wrapper for existing serializer/deserializer instances.
 */
public record SerdeAdapter<T>(Serializer<T> serializer, Deserializer<T> deserializer) implements Serde<T> {
}

