package be.appify.prefab.core.sns;

import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.core.spring.JsonUtil;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

/**
 * A deserializer for SQS messages that dynamically chooses the deserialization method based on the topic's
 * serialization format registered in the {@link EventRegistry}. If the topic is not registered, JSON deserialization
 * is used by default. Type-name allowlist validation is also delegated to the {@link EventRegistry}.
 */
@Component
@ConditionalOnClass(SnsTemplate.class)
public class SqsDeserializer {
    private final JsonUtil jsonUtil;
    private final EventRegistry eventRegistry;
    private final ConversionService conversionService;

    /**
     * Constructs a SqsDeserializer with the given JsonUtil, EventRegistry, and ConversionService.
     *
     * @param jsonUtil           the JsonUtil to use for JSON deserialization
     * @param eventRegistry      the EventRegistry that contains the serialization format for each topic
     * @param conversionService  the ConversionService to convert GenericRecord to the target event class
     */
    public SqsDeserializer(JsonUtil jsonUtil, EventRegistry eventRegistry, ConversionService conversionService) {
        this.jsonUtil = jsonUtil;
        this.eventRegistry = eventRegistry;
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
        if (!eventRegistry.contains(topic)) {
            return deserializeJson(envelope.payload(), envelope.typeName(), type);
        }
        return switch (eventRegistry.serialization(topic)) {
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
            var resolvedType = eventRegistry.typeByClassName(typeName)
                    .orElseThrow(() -> new UnknownEventTypeException(typeName));
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
