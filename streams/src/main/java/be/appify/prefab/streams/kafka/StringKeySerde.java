package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Key;
import java.util.function.Function;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class StringKeySerde<K extends Key<K>> implements Serde<K> {
    private final StringKeySerializer<K> serializer;
    private final StringKeyDeserializer<K> deserializer;

    public StringKeySerde(Class<K> keyType) {
        serializer = new StringKeySerializer<>();
        deserializer = new StringKeyDeserializer<>(key -> Key.parse(key, keyType));
    }

    @Override
    public Serializer<K> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<K> deserializer() {
        return deserializer;
    }

    private static class StringKeySerializer<K> implements Serializer<K> {

        @Override
        public byte[] serialize(String topic, K data) {
            return data.toString().getBytes(UTF_8);
        }
    }

    private record StringKeyDeserializer<K>(Function<String, K> deserializerFunction) implements Deserializer<K> {

        @Override
        public K deserialize(String topic, byte[] data) {
            return deserializerFunction.apply(new String(data, UTF_8));
        }
    }
}
