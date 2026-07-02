package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.core.convert.ConversionService;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON-based {@link Serde} for key types.
 *
 * <p>Serializes keys to JSON using Jackson; deserializes JSON back to strongly-typed key instances.
 * This eliminates the need for manual {@code parse(String)} and {@code toString()} implementations.
 *
 * @param <K> the key type
 */
public class JsonKeySerde<K> implements Serde<K> {
    private final JsonKeySerializer<K> serializer;
    private final JsonKeyDeserializer<K> deserializer;

    /**
     * Constructs a new JsonKeySerde for the given key type.
     *
     * @param keyType the concrete key class
     * @param mapper  the Jackson JsonMapper to use for serialization/deserialization
     */
    public JsonKeySerde(Class<K> keyType, JsonMapper mapper) {
        this(keyType, mapper, null, null, Map.of());
    }

    /**
     * Constructs a new key serde that automatically switches key wire format by topic serialization.
     * AVRO topics use Avro key bytes; all other topics use JSON key bytes.
     */
    public JsonKeySerde(
            Class<K> keyType,
            JsonMapper mapper,
            EventRegistry eventRegistry,
            ConversionService conversionService,
            Map<String, Object> kafkaClientProperties
    ) {
        this.serializer = new JsonKeySerializer<>(mapper, eventRegistry, conversionService, kafkaClientProperties);
        this.deserializer = new JsonKeyDeserializer<>(keyType, mapper, eventRegistry, conversionService, kafkaClientProperties);
    }

    @Override
    public Serializer<K> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<K> deserializer() {
        return deserializer;
    }

    private static class JsonKeySerializer<K> implements Serializer<K> {
        private final JsonMapper mapper;
        private final EventRegistry eventRegistry;
        private final ConversionService conversionService;
        private final GenericAvroSerializer avroSerializer;

        JsonKeySerializer(
                JsonMapper mapper,
                EventRegistry eventRegistry,
                ConversionService conversionService,
                Map<String, Object> kafkaClientProperties
        ) {
            this.mapper = mapper;
            this.eventRegistry = eventRegistry;
            this.conversionService = conversionService;
            this.avroSerializer = new GenericAvroSerializer();
            this.avroSerializer.configure(withSchemaRegistryUrl(kafkaClientProperties), true);
        }

        @Override
        public byte[] serialize(String topic, K data) {
            if (data == null) {
                return null;
            }
            try {
                if (isAvroTopic(topic, eventRegistry)) {
                    return avroSerializer.serialize(topic, toGenericRecord(data));
                }
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to serialize key of type " + data.getClass().getName() + " for topic " + topic,
                        e);
            }
        }

        private GenericRecord toGenericRecord(K data) {
            if (data instanceof GenericRecord record) {
                return record;
            }
            if (conversionService != null && conversionService.canConvert(data.getClass(), GenericRecord.class)) {
                var converted = conversionService.convert(data, GenericRecord.class);
                if (converted != null) {
                    return converted;
                }
            }
            return AvroKeyRecordMapper.toGenericRecord(data);
        }
    }

    private record JsonKeyDeserializer<K>(
            Class<K> keyType,
            JsonMapper mapper,
            EventRegistry eventRegistry,
            ConversionService conversionService,
            GenericAvroDeserializer avroDeserializer
    ) implements Deserializer<K> {

        JsonKeyDeserializer(
                Class<K> keyType,
                JsonMapper mapper,
                EventRegistry eventRegistry,
                ConversionService conversionService,
                Map<String, Object> kafkaClientProperties
        ) {
            this(
                    keyType,
                    mapper,
                    eventRegistry,
                    conversionService,
                    configuredAvroDeserializer(kafkaClientProperties)
            );
        }

        @Override
        public K deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                if (isAvroTopic(topic, eventRegistry)) {
                    var genericRecord = avroDeserializer.deserialize(topic, data);
                    if (genericRecord == null) {
                        return null;
                    }
                    return fromGenericRecord(genericRecord);
                }
                return mapper.readValue(data, keyType);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to deserialize key of type " + keyType.getName() + " from topic " + topic,
                        e);
            }
        }

        @SuppressWarnings("unchecked")
        private K fromGenericRecord(Object genericRecord) {
            if (conversionService != null && conversionService.canConvert(genericRecord.getClass(), keyType)) {
                var converted = conversionService.convert(genericRecord, keyType);
                if (converted != null) {
                    return converted;
                }
            }
            if (genericRecord instanceof GenericRecord record) {
                return AvroKeyRecordMapper.fromGenericRecord(record, keyType);
            }
            return (K) genericRecord;
        }
    }

    private static GenericAvroDeserializer configuredAvroDeserializer(Map<String, Object> kafkaClientProperties) {
        var deserializer = new GenericAvroDeserializer();
        deserializer.configure(withSchemaRegistryUrl(kafkaClientProperties), true);
        return deserializer;
    }

    private static Map<String, Object> withSchemaRegistryUrl(Map<String, Object> kafkaClientProperties) {
        var properties = new HashMap<>(kafkaClientProperties);
        properties.putIfAbsent("schema.registry.url", "mock://schema-url");
        return properties;
    }

    private static boolean isAvroTopic(String topic, EventRegistry eventRegistry) {
        return eventRegistry != null
                && eventRegistry.contains(topic)
                && eventRegistry.serialization(topic) == Event.Serialization.AVRO;
    }
}
