package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.serialization.ConcreteEventSupport;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerializer;
import be.appify.prefab.core.versioning.EventVersions;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.common.header.Headers;
import java.util.concurrent.locks.ReentrantLock;
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
    private final ConversionService conversionService;
    private final EventRegistry eventRegistry;
    private final Map<String, Object> producerProperties;
    private final ReentrantLock avroSerializerLock = new ReentrantLock();
    private GenericAvroSerializer avroSerializer;

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
        this.producerProperties = kafkaProperties.buildProducerProperties();
        jsonSerializer.configure(producerProperties, false);
        if (eventRegistry.hasSerialization(Event.Serialization.AVRO)) {
            this.avroSerializer = configuredAvroSerializer();
        }
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
        ConcreteEventSupport.payloadFor(topic, data, eventRegistry);
        if (data instanceof TypedEvent<?>) {
            throw new IllegalArgumentException("Generic event serialization requires headers for " + EventType.HEADER);
        }
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
                    case AVRO -> avroSerializer().serialize(topic, toGenericRecord(data));
                    case JSON -> jsonSerializer.serialize(topic, data);
                };
            }
        }
    }

    /**
     * Serialize the given data and stamp the version headers of the event on the record.
     *
     * @param topic   the topic to which the data is being serialized
     * @param headers the headers of the record
     * @param data    the data to serialize
     * @return the serialized byte array
     */
    @Override
    public byte[] serialize(String topic, Headers headers, Object data) {
        var payload = ConcreteEventSupport.payloadFor(topic, data, eventRegistry);
        if (data instanceof TypedEvent<?> typed) {
            if (headers == null) {
                throw new IllegalArgumentException("Generic event serialization requires " + EventType.HEADER);
            }
            headers.remove(EventType.HEADER);
            headers.add(EventType.HEADER, typed.type().id().getBytes(StandardCharsets.UTF_8));
            EventVersions.headersFor(typed).forEach((name, value) -> {
                headers.remove(name);
                headers.add(name, value.getBytes(StandardCharsets.UTF_8));
            });
            return jsonSerializer.serialize(topic, payload);
        }
        if (headers != null && data != null && !(data instanceof String) && !(data instanceof byte[])) {
            EventVersions.headersFor(data).forEach((name, value) -> {
                headers.remove(name);
                headers.add(name, value.getBytes(StandardCharsets.UTF_8));
            });
        }
        return serialize(topic, data);
    }

    private GenericAvroSerializer avroSerializer() {
        var serializer = avroSerializer;
        if (serializer != null) {
            return serializer;
        }
        avroSerializerLock.lock();
        try {
            if (avroSerializer == null) {
                avroSerializer = configuredAvroSerializer();
            }
            return avroSerializer;
        } finally {
            avroSerializerLock.unlock();
        }
    }

    private GenericAvroSerializer configuredAvroSerializer() {
        var serializer = new GenericAvroSerializer();
        serializer.configure(KafkaSchemaRegistrySupport.avroClientProperties(producerProperties, eventRegistry), false);
        return serializer;
    }

    private GenericRecord toGenericRecord(Object data) {
        return conversionService.convert(data, GenericRecord.class);
    }

    public EventRegistry eventRegistry() {
        return eventRegistry;
    }

    public <T> Serializer<T> adapt() {
        return (Serializer<T>) this;
    }
}
