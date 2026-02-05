package be.appify.prefab.core.kafka;

import be.appify.prefab.core.util.SerializationRegistry;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.stereotype.Component;

/**
 * A Kafka serializer that dynamically chooses serialization based on the topic's serialization format. It uses a
 * {@link SerializationRegistry} to determine the serialization format for each topic and delegates to the appropriate serializer. For Avro
 * serialization, it converts the input object to a {@link GenericRecord} using a {@link ConversionService} before serialization.
 */
@Component
public class DynamicSerializer implements Serializer<Object> {
    private final StringSerializer stringSerializer = new StringSerializer();
    private final JacksonJsonSerializer<Object> jsonSerializer = new JacksonJsonSerializer<>();
    private final GenericAvroSerializer avroSerializer = new GenericAvroSerializer();
    private final ConversionService conversionService;
    private final SerializationRegistry serializationRegistry;

    /**
     * Constructs a DynamicSerializer and configures the underlying JsonSerializer with the provided Kafka properties.
     *
     * @param kafkaProperties
     *         the Kafka properties to configure the JsonSerializer
     * @param conversionService
     *         the ConversionService to convert objects to GenericRecord for Avro serialization
     * @param serializationRegistry
     *         the SerializationRegistry that contains the serialization format for each topic
     */
    public DynamicSerializer(
            KafkaProperties kafkaProperties,
            ConversionService conversionService,
            SerializationRegistry serializationRegistry
    ) {
        this.conversionService = conversionService;
        this.serializationRegistry = serializationRegistry;
        var producerProperties = kafkaProperties.buildProducerProperties();
        jsonSerializer.configure(producerProperties, false);
        if (!producerProperties.containsKey("schema.registry.url")) {
            producerProperties.put("schema.registry.url", "mock://schema-url");
        }
        avroSerializer.configure(producerProperties, false);
    }

    /**
     * Serialize the given data based on its type.
     *
     * @param topic
     *         the topic to which the data is being serialized
     * @param data
     *         the data to serialize
     * @return the serialized byte array
     */
    @Override
    public byte[] serialize(String topic, Object data) {
        switch (data) {
            case null -> {
                return null;
            }
            case String string -> {
                return stringSerializer.serialize(topic, string);
            }
            case byte[] bytes -> {
                return bytes;
            }
            default -> {
                return switch (serializationRegistry.get(topic)) {
                    case AVRO -> avroSerializer.serialize(topic, toGenericRecord(data));
                    case JSON -> jsonSerializer.serialize(topic, data);
                };
            }
        }
    }

    private GenericRecord toGenericRecord(Object data) {
        if (!conversionService.canConvert(data.getClass(), GenericRecord.class)) {
            throw new IllegalArgumentException("No converter registered for type " + data.getClass() + " to GenericRecord");
        }
        return conversionService.convert(data, GenericRecord.class);
    }
}
