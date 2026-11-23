package be.appify.prefab.streams.kafka;

import be.appify.prefab.streams.StreamKeyEncoding;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class StringKeySerde<K> implements Serde<K> {
    private final StringKeySerializer<K> serializer;
    private final StringKeyDeserializer<K> deserializer;

    public StringKeySerde(Class<K> keyType) {
        var encoding = new StreamKeyEncoding<>(keyType);
        serializer = new StringKeySerializer<>(encoding);
        deserializer = new StringKeyDeserializer<>(encoding);
    }

    @Override
    public Serializer<K> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<K> deserializer() {
        return deserializer;
    }

    private record StringKeySerializer<K>(StreamKeyEncoding<K> encoding) implements Serializer<K> {

        @Override
        public byte[] serialize(String topic, K data) {
            if (data == null) {
                return null;
            }
            return encoding.encode(data);
        }
    }

    private record StringKeyDeserializer<K>(StreamKeyEncoding<K> encoding) implements Deserializer<K> {

        @Override
        public K deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            return encoding.decode(data);
        }
    }
}
