package be.appify.prefab.core.kafka;

import be.appify.prefab.core.util.SerializationRegistry;
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer;
import java.util.NoSuchElementException;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonTypeResolver;
import org.springframework.stereotype.Component;

/**
 * A deserializer that dynamically chooses deserialization based on the topic's serialization format.
 * It uses a {@link SerializationRegistry} to determine the serialization format for each topic and delegates to the appropriate deserializer.
 * For Avro deserialization, it converts the resulting {@link GenericRecord} to the target event class using a {@link ConversionService}.
 */
@Component
public class DynamicDeserializer implements Deserializer<Object> {
    private final JacksonJsonDeserializer<Object> jsonDeserializer = new JacksonJsonDeserializer<>();
    private final GenericAvroDeserializer avroDeserializer = new GenericAvroDeserializer();
    private final ConversionService conversionService;
    private final SerializationRegistry serializationRegistry;

    /**
     * Constructs a DynamicDeserializer and configures the underlying JsonDeserializer and AvroDeserializer with the provided Kafka properties.
     *
     * @param kafkaProperties
     *         the Kafka properties to configure the deserializers
     * @param conversionService
     *         the ConversionService to convert GenericRecord to the target event class for Avro deserialization
     * @param serializationRegistry
     *         the SerializationRegistry that contains the serialization format for each topic
     * @param jsonTypeResolver
     *         the JacksonJsonTypeResolver to resolve types for JSON deserialization
     */
    public DynamicDeserializer(
            KafkaProperties kafkaProperties,
            ConversionService conversionService,
            SerializationRegistry serializationRegistry,
            JacksonJsonTypeResolver jsonTypeResolver
    ) {
        this.conversionService = conversionService;
        this.serializationRegistry = serializationRegistry;
        var consumerProperties = kafkaProperties.buildConsumerProperties();
        jsonDeserializer.setTypeResolver(jsonTypeResolver);
        jsonDeserializer.configure(consumerProperties, false);
        if (!consumerProperties.containsKey("schema.registry.url")) {
            consumerProperties.put("schema.registry.url", "mock://schema-url");
        }
        avroDeserializer.configure(consumerProperties, false);
    }

    /**
     * Deserialize the given byte array based on the topic's serialization format.
     *
     * @param topic
     *         the topic from which the data is being deserialized
     * @param data
     *         the byte array to deserialize
     * @return the deserialized object
     */
    @Override
    public Object deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        } else {
            return switch (serializationRegistry.get(topic)) {
                case AVRO -> toEvent(avroDeserializer.deserialize(topic, data));
                case JSON -> jsonDeserializer.deserialize(topic, data);
            };
        }
    }

    private Object toEvent(GenericRecord genericRecord) {
        var schema = genericRecord.getSchema();
        Class<?> targetClass;
        try {
            targetClass = Class.forName(schema.getFullName().replace('_', '$'));
        } catch (ClassNotFoundException e) {
             throw new NoSuchElementException(schema.getFullName(), e);
        }
        if(!conversionService.canConvert(GenericRecord.class, targetClass)) {
            throw new IllegalArgumentException("No converter registered for GenericRecord to " + targetClass);
        }
        return conversionService.convert(genericRecord, targetClass);
    }
}
