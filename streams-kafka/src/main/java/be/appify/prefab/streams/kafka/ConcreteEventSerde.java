package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/** Header-aware publication serde retaining the complete event binding. */
public final class ConcreteEventSerde<T> implements Serde<T> {
    private final EventType<T> type;
    private final DynamicSerializer writer;
    private final DynamicDeserializer reader;

    /** @param type binding @param writer transport serializer @param reader transport deserializer */
    public ConcreteEventSerde(EventType<T> type, DynamicSerializer writer, DynamicDeserializer reader) {
        this.type = type;
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    public Serializer<T> serializer() {
        return new Serializer<>() {
            public byte[] serialize(String topic, T value) {
                return serialize(topic, new RecordHeaders(), value);
            }
            public byte[] serialize(String topic, Headers headers, T value) {
                return writer.serialize(topic, headers, value == null || type.type() instanceof Class<?>
                        ? value : new TypedEvent<>(type, value));
            }
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return new Deserializer<>() {
            public T deserialize(String topic, byte[] data) {
                throw new IllegalArgumentException("Concrete event deserialization requires headers for " + type);
            }
            @SuppressWarnings("unchecked")
            public T deserialize(String topic, Headers headers, byte[] data) {
                if (data == null) {
                    return null;
                }
                if (type.type() instanceof Class<?> raw) {
                    var value = reader.deserialize(topic, headers, data);
                    if (!raw.isInstance(value)) {
                        throw new IllegalArgumentException("Message does not match stream binding " + type);
                    }
                    return (T) value;
                }
                var header = headers == null ? null : headers.lastHeader(EventType.HEADER);
                if (header == null || !type.id().equals(new String(header.value(),
                        StandardCharsets.UTF_8))) {
                    throw new IllegalArgumentException("Message does not match concrete stream binding " + type);
                }
                return (T) reader.deserialize(topic, headers, data);
            }
        };
    }
}
