package be.appify.prefab.core.kafka;

import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.type.TypeFactory;

/**
 * A deserializer that dynamically chooses deserialization based on the topic's serialization format.
 * It uses an {@link EventRegistry} to determine the serialization format for each topic and delegates to the appropriate deserializer.
 * For Avro deserialization, it converts the resulting {@link GenericRecord} to the target event class using a {@link ConversionService}.
 */
public class DynamicDeserializer implements Deserializer<Object> {
    private static final Logger log = LoggerFactory.getLogger(DynamicDeserializer.class);

    private final JacksonJsonDeserializer<Object> jsonDeserializer = new JacksonJsonDeserializer<>();
    private final GenericAvroDeserializer avroDeserializer = new GenericAvroDeserializer();
    private final ConversionService conversionService;
    private final EventRegistry eventRegistry;

    /**
     * Constructs a DynamicDeserializer and configures the underlying JsonDeserializer and AvroDeserializer.
     *
     * @param kafkaProperties   the Kafka properties to configure the deserializers
     * @param conversionService the ConversionService to convert GenericRecord to the target event class
     * @param eventRegistry     the EventRegistry for type resolution and serialization-format lookup
     */
    public DynamicDeserializer(
            KafkaProperties kafkaProperties,
            ConversionService conversionService,
            EventRegistry eventRegistry
    ) {
        this.conversionService = conversionService;
        this.eventRegistry = eventRegistry;
        var consumerProperties = kafkaProperties.buildConsumerProperties();
        jsonDeserializer.setTypeResolver(
                (topic, data, headers) -> TypeFactory.unsafeSimpleType(eventRegistry.typeFor(topic)));
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
            try {
                return switch (eventRegistry.serialization(topic)) {
                    case AVRO -> toEvent(topic, avroDeserializer.deserialize(topic, data));
                    case JSON -> jsonDeserializer.deserialize(topic, data);
                };
            } catch (UnknownEventTypeException e) {
                throw e;
            } catch (RuntimeException e) {
                if (isUnknownTypeIdException(e)) {
                    throw new UnknownEventTypeException(extractTypeId(e));
                }
                log.error("Failed to deserialize message from topic [{}]", topic, e);
                throw e;
            }
        }
    }

    private Object toEvent(String topic, GenericRecord genericRecord) {
        var schema = genericRecord.getSchema();
        var targetClass = resolveTargetClass(topic, schema.getName(), schema.getFullName());
        if (!conversionService.canConvert(GenericRecord.class, targetClass)) {
            throw new IllegalArgumentException("No converter registered for GenericRecord to " + targetClass);
        }
        return conversionService.convert(genericRecord, targetClass);
    }

    private Class<?> resolveTargetClass(String topic, String schemaName, String schemaFullName) {
        var candidates = eventRegistry.registeredTypesForTopic(topic).stream()
                .filter(type -> typeNameMatchesSchema(type, schemaName))
                .toList();

        if (candidates.isEmpty()) {
            throw new UnknownEventTypeException(schemaFullName);
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("Multiple event types registered for schema "
                    + schemaFullName + " on topic " + topic + ": " + candidates);
        }

        return candidates.getFirst();
    }

    private boolean typeNameMatchesSchema(Class<?> type, String schemaName) {
        var normalizedSchemaName = schemaName.replace('_', '$');
        return type.getName().endsWith("." + normalizedSchemaName);
    }

    public <T> Deserializer<T> adapt() {
        return (Deserializer<T>) this;
    }

    private static boolean isUnknownTypeIdException(Throwable e) {
        while (e != null) {
            if ("tools.jackson.databind.exc.InvalidTypeIdException".equals(e.getClass().getName())) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    private static String extractTypeId(RuntimeException e) {
        var cause = (Throwable) e;
        while (cause != null) {
            if ("tools.jackson.databind.exc.InvalidTypeIdException".equals(cause.getClass().getName())) {
                return cause.getMessage() != null ? cause.getMessage() : "unknown";
            }
            cause = cause.getCause();
        }
        return "unknown";
    }
}
