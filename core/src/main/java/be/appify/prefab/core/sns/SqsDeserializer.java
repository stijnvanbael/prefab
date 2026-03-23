package be.appify.prefab.core.sns;

import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.util.SerializationRegistry;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

/**
 * A deserializer for SQS messages that dynamically chooses the deserialization method based on the topic's
 * serialization format. It uses a {@link SerializationRegistry} to determine the serialization format for each topic
 * and delegates to the appropriate deserialization method. If the topic is not registered, JSON deserialization is used
 * by default.
 */
@Component
@ConditionalOnClass(SnsTemplate.class)
public class SqsDeserializer {
    private final JsonUtil jsonUtil;
    private final SerializationRegistry serializationRegistry;
    private final ConversionService conversionService;
    private final ConcurrentMap<String, Class<?>> typeCache = new ConcurrentHashMap<>();

    /**
     * Constructs a SqsDeserializer with the given JsonUtil, SerializationRegistry, and ConversionService.
     *
     * @param jsonUtil
     *         the JsonUtil to use for JSON deserialization
     * @param serializationRegistry
     *         the SerializationRegistry that contains the serialization format for each topic
     * @param conversionService
     *         the ConversionService to convert GenericRecord to the target event class for Avro deserialization
     */
    public SqsDeserializer(JsonUtil jsonUtil, SerializationRegistry serializationRegistry, ConversionService conversionService) {
        this.jsonUtil = jsonUtil;
        this.serializationRegistry = serializationRegistry;
        this.conversionService = conversionService;
    }

    /**
     * Deserialize the given raw SQS message body based on the topic's serialization format. The body may be an SNS
     * notification envelope (with {@code Message} and optional {@code Subject} fields) or a raw payload.
     *
     * @param topic
     *         the topic from which the data is being deserialized
     * @param body
     *         the raw SQS message body to deserialize
     * @param type
     *         the base class to deserialize into
     * @param <T>
     *         the type of the object to return
     * @return the deserialized object
     */
    public <T> T deserialize(String topic, String body, Class<T> type) {
        var envelope = extractEnvelope(body);
        if (!serializationRegistry.contains(topic)) {
            return deserializeJson(envelope.payload(), envelope.typeName(), type);
        }
        return switch (serializationRegistry.get(topic)) {
            case AVRO -> deserializeAvro(envelope.payload(), type);
            case JSON -> deserializeJson(envelope.payload(), envelope.typeName(), type);
        };
    }

    private record SnsEnvelope(String payload, String typeName) {
    }

    @SuppressWarnings("unchecked")
    private SnsEnvelope extractEnvelope(String body) {
        try {
            var parsed = (Map<String, Object>) jsonUtil.parseJson(body, Map.class);
            var payload = parsed.containsKey("Message") ? (String) parsed.get("Message") : body;
            var typeName = parsed.containsKey("Subject") ? (String) parsed.get("Subject") : null;
            return new SnsEnvelope(payload, typeName);
        } catch (Exception e) {
            return new SnsEnvelope(body, null);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeJson(String payload, String typeName, Class<T> type) {
        if (typeName != null) {
            var resolvedType = typeCache.computeIfAbsent(typeName, key -> {
                try {
                    return Class.forName(typeName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Could not find class for type: " + typeName, e);
                }
            });
            if (type.isAssignableFrom(resolvedType)) {
                return (T) jsonUtil.parseJson(payload, resolvedType);
            }
        }
        return jsonUtil.parseJson(payload, type);
    }

    private <T> T deserializeAvro(String payload, Class<T> type) {
        var bytes = Base64.getDecoder().decode(payload);
        var avroReader = new GenericDatumReader<GenericRecord>();
        var decoder = new DecoderFactory().binaryDecoder(bytes, null);
        GenericRecord genericRecord;
        try {
            genericRecord = avroReader.read(null, decoder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize data from Avro", e);
        }
        return conversionService.convert(genericRecord, type);
    }
}
