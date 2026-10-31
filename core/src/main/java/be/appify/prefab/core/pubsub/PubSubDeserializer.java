package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.serialization.ConcreteAvroSupport;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.versioning.AvroTrees;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.MessageVersions;
import be.appify.prefab.core.spring.JsonUtil;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * A deserializer for Pub/Sub messages that dynamically chooses the deserialization method based on the topic's serialization format. It uses a
 * {@link SerializationRegistry} to determine the serialization format for each topic and delegates to the appropriate deserialization method.
 */
@Component
public class PubSubDeserializer {
    private final JsonUtil jsonUtil;
    private final EventRegistry eventRegistry;
    private final ConversionService conversionService;
    private final EventMigrator eventMigrator;
    private final JsonMapper jsonMapper;

    /**
     * Constructs a PubSubDeserializer with the given JsonUtil, EventRegistry, and ConversionService.
     *
     * @param jsonUtil       the JsonUtil to use for JSON deserialization
     * @param eventRegistry  the EventRegistry that contains the serialization format for each topic
     * @param conversionService the ConversionService to convert GenericRecord to the target event class
     */
    public PubSubDeserializer(JsonUtil jsonUtil, EventRegistry eventRegistry, ConversionService conversionService) {
        this(jsonUtil, eventRegistry, conversionService, EventMigrator.none(), JsonMapper.builder().build());
    }

    /**
     * Constructs a PubSubDeserializer that migrates messages written with an older version of the event contract.
     *
     * @param jsonUtil          the JsonUtil to use for JSON deserialization
     * @param eventRegistry     the EventRegistry that contains the serialization format for each topic
     * @param conversionService the ConversionService to convert GenericRecord to the target event class
     * @param eventMigrator     the migrator that reads messages written with an older version
     * @param jsonMapper        the mapper that binds migrated payloads to snapshot types
     */
    @Autowired
    public PubSubDeserializer(
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
     * Deserialize the given data based on the topic's serialization format.
     *
     * @param topic
     *         the topic from which the data is being deserialized
     * @param data
     *         the data to deserialize
     * @param type
     *         the class of the object to return
     * @param <T>
     *         the type of the object to return
     * @return the deserialized object
     */
    public <T> T deserialize(String topic, ByteString data, Class<T> type) {
        return deserialize(topic, data, type, Map.of());
    }

    /**
     * Deserialize the given data, reading the version attributes of the message to migrate older messages.
     *
     * @param topic      the topic from which the data is being deserialized
     * @param data       the data to deserialize
     * @param type       the class of the object to return
     * @param attributes the attributes of the Pub/Sub message
     * @param <T>        the type of the object to return
     * @return the deserialized object
     * @throws be.appify.prefab.core.versioning.UnsupportedEventVersionException if the message was written with a version that cannot be read
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(String topic, ByteString data, Class<T> type, Map<String, String> attributes) {
        if (type.getTypeParameters().length > 0) {
            throw new IllegalArgumentException("Generic event deserialization requires an EventType: " + type.getName());
        }
        var serialization = eventRegistry.serialization(topic);
        var eventType = Optional.ofNullable(attributes.get("type")).flatMap(eventRegistry::typeByClassName)
                .filter(type::isAssignableFrom).orElse((Class) type);
        var plan = eventMigrator.plan(eventType, MessageVersions.from(attributes::get));
        if (plan.isRequired()) {
            var tree = switch (serialization) {
                case AVRO -> AvroTrees.toTree(readAvro(data.toByteArray()), jsonMapper);
                case JSON -> jsonMapper.readTree(data.toStringUtf8());
            };
            return (T) plan.apply(tree, jsonMapper);
        }
        return switch (serialization) {
            case AVRO -> deserializeAvro(data.toByteArray(), type);
            case JSON -> jsonUtil.parseJson(data.toStringUtf8(), type);
        };
    }

    /**
     * Reads a JSON message for exactly one registered concrete binding.
     *
     * @param topic source topic
     * @param data message payload
     * @param type consumer binding
     * @param attributes transport metadata
     * @param <T> event type
     * @return the concretely bound payload
     */
    public <T> T deserialize(String topic, ByteString data, EventType<T> type, Map<String, String> attributes) {
        var identity = attributes.get(EventType.HEADER);
        if (identity == null) {
            throw new IllegalArgumentException("Generic event is missing " + EventType.HEADER);
        }
        var registered = eventRegistry.concreteTypeFor(topic, identity);
        if (!registered.equals(type)) {
            throw new IllegalArgumentException("Message does not match consumer binding " + type);
        }
        var plan = eventMigrator.plan(type, MessageVersions.from(attributes::get));
        if (plan.isRequired()) {
            if (eventRegistry.serialization(topic) == Event.Serialization.AVRO) {
                throw new IllegalArgumentException("Avro migration on Pub/Sub requires a writer schema (TASK-275): " + type);
            }
            @SuppressWarnings("unchecked")
            var migrated = (T) plan.apply(jsonMapper.readTree(data.toStringUtf8()), jsonMapper);
            return migrated;
        }
        return switch (eventRegistry.serialization(topic)) {
            case JSON -> jsonUtil.parseJson(data.toStringUtf8(), type);
            case AVRO -> ConcreteAvroSupport.read(data.toByteArray(), type, eventRegistry);
        };
    }

    private <T> T deserializeAvro(byte[] data, Class<T> type) {
        return conversionService.convert(readAvro(data), type);
    }

    private GenericRecord readAvro(byte[] data) {
        var avroReader = new GenericDatumReader<GenericRecord>();
        var decoder = new DecoderFactory().binaryDecoder(data, null);
        try {
            return avroReader.read(null, decoder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize data from Avro", e);
        }
    }
}
