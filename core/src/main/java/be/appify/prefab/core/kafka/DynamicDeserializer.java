package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PreviousName;
import be.appify.prefab.core.evolution.EventEvolutionModule;
import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.kafka.support.JacksonMapperUtils;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

/**
 * A deserializer that dynamically chooses deserialization based on the topic's serialization format.
 * It uses an {@link EventRegistry} to determine the serialization format for each topic and delegates to the appropriate deserializer.
 * For Avro deserialization, it converts the resulting {@link GenericRecord} to the target event class using a {@link ConversionService}.
 */
public class DynamicDeserializer implements Deserializer<Object> {
    private static final Logger log = LoggerFactory.getLogger(DynamicDeserializer.class);

    private final JacksonJsonDeserializer<Object> jsonDeserializer = new JacksonJsonDeserializer<>(evolutionAwareMapper());
    private final ConversionService conversionService;
    private final EventRegistry eventRegistry;
    private final Map<String, Object> consumerProperties;
    private final ReentrantLock avroDeserializerLock = new ReentrantLock();
    private GenericAvroDeserializer avroDeserializer;

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
        this.consumerProperties = kafkaProperties.buildConsumerProperties();
        jsonDeserializer.setTypeResolver(
                (topic, data, headers) -> TypeFactory.unsafeSimpleType(eventRegistry.typeFor(topic)));
        jsonDeserializer.configure(consumerProperties, false);
        if (eventRegistry.hasSerialization(Event.Serialization.AVRO)) {
            this.avroDeserializer = configuredAvroDeserializer();
        }
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
                    case AVRO -> toEvent(topic, avroDeserializer().deserialize(topic, data));
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

    private static JsonMapper evolutionAwareMapper() {
        return JacksonMapperUtils.enhancedJsonMapper().rebuild().addModule(new EventEvolutionModule()).build();
    }

    private GenericAvroDeserializer avroDeserializer() {
        var deserializer = avroDeserializer;
        if (deserializer != null) {
            return deserializer;
        }
        avroDeserializerLock.lock();
        try {
            if (avroDeserializer == null) {
                avroDeserializer = configuredAvroDeserializer();
            }
            return avroDeserializer;
        } finally {
            avroDeserializerLock.unlock();
        }
    }

    private GenericAvroDeserializer configuredAvroDeserializer() {
        var deserializer = new GenericAvroDeserializer();
        deserializer.configure(KafkaSchemaRegistrySupport.avroClientProperties(consumerProperties, eventRegistry), false);
        return deserializer;
    }

    private Object toEvent(String topic, GenericRecord genericRecord) {
        var schema = genericRecord.getSchema();
        var targetClass = resolveTargetClass(topic, schema.getName(), schema.getFullName());
        if (!conversionService.canConvert(GenericRecord.class, targetClass)) {
            throw new IllegalArgumentException("No converter registered for GenericRecord to " + targetClass);
        }
        return conversionService.convert(resolveAgainstReaderSchema(targetClass, genericRecord), targetClass);
    }

    /**
     * Applies Avro's reader/writer schema resolution so that defaults, aliases and type promotion declared on the
     * reader schema of the event type apply to messages written with another version of the schema. A message that
     * cannot be resolved is converted as written.
     */
    private GenericRecord resolveAgainstReaderSchema(Class<?> targetClass, GenericRecord genericRecord) {
        var writerSchema = genericRecord.getSchema();
        var readerSchema = eventRegistry.readerSchemaFor(targetClass).orElse(null);
        if (readerSchema == null || readerSchema.equals(writerSchema)) {
            return genericRecord;
        }
        try {
            var output = new ByteArrayOutputStream();
            var encoder = EncoderFactory.get().binaryEncoder(output, null);
            new GenericDatumWriter<GenericRecord>(writerSchema).write(genericRecord, encoder);
            encoder.flush();
            var decoder = DecoderFactory.get().binaryDecoder(output.toByteArray(), null);
            return new GenericDatumReader<GenericRecord>(writerSchema, readerSchema).read(null, decoder);
        } catch (IOException | AvroRuntimeException e) {
            log.warn("Schema [{}] could not be resolved against the schema of {}; converting the message as written",
                    writerSchema.getFullName(), targetClass.getName(), e);
            return genericRecord;
        }
    }

    private Class<?> resolveTargetClass(String topic, String schemaName, String schemaFullName) {
        var registeredTypes = eventRegistry.registeredTypesForTopic(topic);
        var candidates = matching(registeredTypes, type -> typeNameMatchesSchema(type, schemaName));
        if (candidates.isEmpty()) {
            candidates = matching(registeredTypes, type -> previousNameMatchesSchema(type, schemaName, schemaFullName));
        }

        if (candidates.isEmpty()) {
            throw new UnknownEventTypeException(schemaFullName);
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("Multiple event types registered for schema "
                    + schemaFullName + " on topic " + topic + ": " + candidates);
        }

        return candidates.getFirst();
    }

    private static List<Class<?>> matching(Collection<Class<?>> types, Predicate<Class<?>> predicate) {
        return types.stream().filter(predicate).toList();
    }

    private boolean typeNameMatchesSchema(Class<?> type, String schemaName) {
        var normalizedSchemaName = schemaName.replace('_', '$');
        return type.getName().endsWith("." + normalizedSchemaName);
    }

    private boolean previousNameMatchesSchema(Class<?> type, String schemaName, String schemaFullName) {
        var previousNames = Arrays.stream(type.getAnnotationsByType(PreviousName.class))
                .flatMap(annotation -> Arrays.stream(annotation.value()))
                .toList();
        var readerAliases = eventRegistry.readerSchemaFor(type).map(Schema::getAliases).orElseGet(Set::of);
        return previousNames.stream().anyMatch(name -> name.equals(schemaFullName) || name.equals(schemaName))
                || readerAliases.contains(schemaFullName);
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
