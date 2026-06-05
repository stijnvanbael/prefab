package be.appify.prefab.core.kafka;

import io.confluent.kafka.streams.serdes.avro.GenericAvroSerializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

/**
 * A Kafka serializer that dynamically chooses serialization based on the topic's serialization format. It uses an
 * {@link EventRegistry} to determine the serialization format for each topic and delegates to the appropriate serializer. For Avro
 * serialization, it converts the input object to a {@link GenericRecord} using a {@link ConversionService} before serialization.
 */
public class DynamicSerializer implements Serializer<Object> {
    private final StringSerializer stringSerializer = new StringSerializer();
    private final JacksonJsonSerializer<Object> jsonSerializer = new JacksonJsonSerializer<>();
    private final GenericAvroSerializer avroSerializer = new GenericAvroSerializer();
    private final ConversionService conversionService;
    private final EventRegistry eventRegistry;

    /**
     * Constructs a DynamicSerializer and configures the underlying JsonSerializer with the provided Kafka properties.
     *
     * @param kafkaProperties the Kafka properties to configure the JsonSerializer
     * @param conversionService the ConversionService to convert objects to GenericRecord for Avro serialization
     * @param eventRegistry the EventRegistry that contains the serialization format for each topic
     */
    public DynamicSerializer(
            KafkaProperties kafkaProperties,
            ConversionService conversionService,
            EventRegistry eventRegistry
    ) {
        this.conversionService = conversionService;
        this.eventRegistry = eventRegistry;
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
                if (!eventRegistry.contains(topic)) {
                    return jsonSerializer.serialize(topic, data);
                }
                return switch (eventRegistry.serialization(topic)) {
                    case AVRO -> avroSerializer.serialize(topic, toGenericRecord(data));
                    case JSON -> jsonSerializer.serialize(topic, data);
                };
            }
        }
    }

    private GenericRecord toGenericRecord(Object data) {
        return conversionService.convert(data, GenericRecord.class);
    }

    public <T> Serializer<T> adapt() {
        return (Serializer<T>) this;
    }
}
