package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.versioning.AvroTrees;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.MessageVersions;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.core.spring.JsonUtil;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

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
    private final EventMigrator eventMigrator;
    private final JsonMapper jsonMapper;

    /**
     * Constructs a SqsDeserializer with the given JsonUtil, EventRegistry, and ConversionService.
     *
     * @param jsonUtil           the JsonUtil to use for JSON deserialization
     * @param eventRegistry      the EventRegistry that contains the serialization format for each topic
     * @param conversionService  the ConversionService to convert GenericRecord to the target event class
     */
    public SqsDeserializer(JsonUtil jsonUtil, EventRegistry eventRegistry, ConversionService conversionService) {
        this(jsonUtil, eventRegistry, conversionService, EventMigrator.none(), JsonMapper.builder().build());
    }

    /**
     * Constructs a SqsDeserializer that migrates messages written with an older version of the event contract.
     *
     * @param jsonUtil          the JsonUtil to use for JSON deserialization
     * @param eventRegistry     the EventRegistry that contains the serialization format for each topic
     * @param conversionService the ConversionService to convert GenericRecord to the target event class
     * @param eventMigrator     the migrator that reads messages written with an older version
     * @param jsonMapper        the mapper that binds migrated payloads to snapshot types
     */
    @Autowired
    public SqsDeserializer(
            JsonUtil jsonUtil,
            EventRegistry eventRegistry,
            ConversionService conversionService,
            EventMigrator eventMigrator,
            JsonMapper jsonMapper
    ) {
        this.jsonUtil = jsonUtil;
        this.eventRegistry = eventRegistry;
        this.conversionService = conversionService;
        this.eventMigrator = eventMigrator;
        this.jsonMapper = jsonMapper;
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
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String topic, String body, Class<T> type) {
        var envelope = extractEnvelope(body);
        var serialization = eventRegistry.contains(topic) ? eventRegistry.serialization(topic) : Event.Serialization.JSON;
        var eventType = resolveEventType(envelope.typeName(), type);
        var plan = eventMigrator.plan(eventType, MessageVersions.from(envelope.attributes()::get));
        if (plan.isRequired()) {
            var tree = serialization == Event.Serialization.AVRO
                    ? AvroTrees.toTree(readAvro(envelope.payload()), jsonMapper)
                    : jsonMapper.readTree(envelope.payload());
            return (T) plan.apply(tree, jsonMapper);
        }
        return switch (serialization) {
            case AVRO -> conversionService.convert(readAvro(envelope.payload()), type);
            case JSON -> deserializeJson(envelope.payload(), envelope.typeName(), type);
        };
    }

    private Class<?> resolveEventType(String typeName, Class<?> type) {
        return Optional.ofNullable(typeName).flatMap(eventRegistry::typeByClassName)
                .filter(type::isAssignableFrom).orElse((Class) type);
    }

    private record SnsEnvelope(String payload, String typeName, Map<String, String> attributes) {
    }

    @SuppressWarnings("unchecked")
    private SnsEnvelope extractEnvelope(String body) {
        try {
            var parsed = (Map<String, Object>) jsonUtil.parseJson(body, Map.class);
            var payload = parsed.containsKey("Message") ? (String) parsed.get("Message") : body;
            var typeName = parsed.containsKey("Subject") ? (String) parsed.get("Subject") : null;
            return new SnsEnvelope(payload, typeName, messageAttributes(parsed.get("MessageAttributes")));
        } catch (Exception e) {
            return new SnsEnvelope(body, null, Map.of());
        }
    }

    private static Map<String, String> messageAttributes(Object attributes) {
        var result = new HashMap<String, String>();
        if (attributes instanceof Map<?, ?> map) {
            map.forEach((name, attribute) -> {
                if (attribute instanceof Map<?, ?> details && details.get("Value") != null) {
                    result.put(name.toString(), details.get("Value").toString());
                }
            });
        }
        return result;
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


    private GenericRecord readAvro(String payload) {
        var bytes = Base64.getDecoder().decode(payload);
        var avroReader = new GenericDatumReader<GenericRecord>();
        var decoder = new DecoderFactory().binaryDecoder(bytes, null);
        try {
            return avroReader.read(null, decoder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize data from Avro", e);
        }
    }
}
