package be.appify.prefab.streams.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/** Lightweight serde wrapper for existing serializer/deserializer instances. */
public final class SerdeAdapter<T> implements Serde<T> {
    private final Serializer<T> serializer;
    private final Deserializer<T> deserializer;

    /**
     * Constructs a new SerdeAdapter.
     *
     * @param serializer
     *         serializer delegate
     * @param deserializer
     *         deserializer delegate
     */
    public SerdeAdapter(Serializer<T> serializer, Deserializer<T> deserializer) {
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }
}

