package be.appify.prefab.streams.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.json.JsonMapper;

/** Registry-independent JSON serde for values whose type is known statically. */
final class JsonValueSerde<T> implements Serde<T> {
    private final JsonMapper jsonMapper;
    private final Class<T> type;

    JsonValueSerde(JsonMapper jsonMapper, Class<T> type) {
        this.jsonMapper = jsonMapper;
        this.type = type;
    }

    @Override
    public Serializer<T> serializer() {
        return (topic, value) -> value == null ? null : jsonMapper.writeValueAsBytes(value);
    }

    @Override
    public Deserializer<T> deserializer() {
        return (topic, data) -> data == null ? null : jsonMapper.readValue(data, type);
    }
}
