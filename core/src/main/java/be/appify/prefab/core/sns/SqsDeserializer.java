package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.serialization.ConcreteAvroSupport;
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
import tools.jackson.databind.JsonNode;

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
        if (type.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Generic event deserialization requires an EventType: " + type.getName());
        }
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

    /**
     * Reads a notification for one registered concrete binding, skipping other bindings on the same topic.
     *
     * @param topic source topic
     * @param body SNS notification envelope
     * @param type consumer binding
     * @param <T> event type
     * @return the bound payload, empty only for a different registered binding
     */
    public <T> Optional<T> deserializeMatching(String topic, String body, EventType<T> type) {
        var envelope = extractEnvelope(body);
        var identity = envelope.attributes().get(EventType.HEADER);
        if (identity == null) {
            throw new IllegalArgumentException("Generic event is missing " + EventType.HEADER);
        }
        var registered = eventRegistry.concreteTypeFor(topic, identity);
        if (!registered.equals(type)) {
            return Optional.empty();
        }
        var plan = eventMigrator.plan(type, MessageVersions.from(envelope.attributes()::get));
        if (plan.isRequired()) {
            if (eventRegistry.serialization(topic) == Event.Serialization.AVRO) {
                throw new IllegalArgumentException("Avro migration on SQS requires a writer schema (TASK-275): " + type);
            }
            @SuppressWarnings("unchecked")
            var migrated = (T) plan.apply(jsonMapper.readTree(envelope.payload()), jsonMapper);
            return Optional.of(migrated);
        }
        return Optional.of(switch (eventRegistry.serialization(topic)) {
            case JSON -> jsonUtil.parseJson(envelope.payload(), type);
            case AVRO -> ConcreteAvroSupport.read(Base64.getDecoder().decode(envelope.payload()), type, eventRegistry);
        });
    }

    private Class<?> resolveEventType(String typeName, Class<?> type) {
        return Optional.ofNullable(typeName).flatMap(eventRegistry::typeByClassName)
                .filter(type::isAssignableFrom).orElse((Class) type);
    }

    private record SnsEnvelope(String payload, String typeName, Map<String, String> attributes) {
    }

    private SnsEnvelope extractEnvelope(String body) {
        if (!body.stripLeading().startsWith("{")) {
            return new SnsEnvelope(body, null, Map.of());
        }
        var parsed = jsonMapper.readTree(body);
        if (!parsed.isObject() || !parsed.has("Message")) {
            return new SnsEnvelope(body, null, Map.of());
        }
        var message = parsed.get("Message");
        if (!message.isString()) {
            throw new IllegalArgumentException("SNS notification Message must be a string");
        }
        var subject = parsed.get("Subject");
        return new SnsEnvelope(message.asText(), subject == null ? null : subject.asText(),
                messageAttributes(parsed.get("MessageAttributes")));
    }

    private static Map<String, String> messageAttributes(JsonNode attributes) {
        var result = new HashMap<String, String>();
        if (attributes != null && attributes.isObject()) {
            attributes.properties().forEach(entry -> {
                var value = entry.getValue().get("Value");
                if (value != null && value.isString()) {
                    result.put(entry.getKey(), value.asText());
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
