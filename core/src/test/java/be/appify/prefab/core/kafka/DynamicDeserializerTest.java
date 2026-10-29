package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PreviousName;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.GenericConversionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicDeserializerTest {

    @Test
    void avroToEventDoesNotRequireJsonTypeAllowlist() {
        var conversionService = new GenericConversionService();
        conversionService.addConverter(GenericRecord.class, AvroEvent.class,
                source -> new AvroEvent(source.get("value").toString()));

        var eventRegistry = new EventRegistry();
        eventRegistry.register("avro-topic", AvroEvent.class, Event.Serialization.AVRO);
        var deserializer = createDeserializer(conversionService, eventRegistry);
        var genericRecord = recordWithSchema("different.namespace", "DynamicDeserializerTest_AvroEvent", "allowed");

        var event = invokeToEvent(deserializer, genericRecord);

        assertInstanceOf(AvroEvent.class, event);
        assertEquals("allowed", ((AvroEvent) event).value());
    }

    @Test
    void avroToEventThrowsUnknownEventTypeExceptionForUnregisteredSchema() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("avro-topic", AvroEvent.class, Event.Serialization.AVRO);
        var deserializer = createDeserializer(new GenericConversionService(), eventRegistry);
        var genericRecord = recordWithSchema("be.appify.prefab.core.kafka", "UnknownAvroEvent", "value");

        var exception = assertThrows(UnknownEventTypeException.class, () -> invokeToEvent(deserializer, genericRecord));

        assertEquals("be.appify.prefab.core.kafka.UnknownAvroEvent", exception.eventTypeName());
    }

    @Test
    void avroToEventFailsWhenConverterIsMissing() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("avro-topic", AvroEvent.class, Event.Serialization.AVRO);
        var deserializer = createDeserializer(new GenericConversionService(), eventRegistry);
        var genericRecord = recordWithSchema("be.appify.prefab.core.kafka", "DynamicDeserializerTest_AvroEvent", "value");

        var exception = assertThrows(IllegalArgumentException.class, () -> invokeToEvent(deserializer, genericRecord));

        assertEquals(
                "No converter registered for GenericRecord to class be.appify.prefab.core.kafka.DynamicDeserializerTest$AvroEvent",
                exception.getMessage()
        );
    }

    @Test
    void avroToEventResolvesPermittedSubclassOfRegisteredSealedInterface() {
        var conversionService = new GenericConversionService();
        conversionService.addConverter(GenericRecord.class, ConcreteAvroEvent.class,
                source -> new ConcreteAvroEvent(source.get("value").toString()));

        var eventRegistry = new EventRegistry();
        eventRegistry.register("avro-topic", SealedAvroEvents.class, Event.Serialization.AVRO);
        var deserializer = createDeserializer(conversionService, eventRegistry);
        var genericRecord = recordWithSchema(
                "be.appify.prefab.core.kafka",
                "DynamicDeserializerTest_ConcreteAvroEvent",
                "payload");

        var event = invokeToEvent(deserializer, genericRecord);

        assertInstanceOf(ConcreteAvroEvent.class, event);
        assertEquals("payload", ((ConcreteAvroEvent) event).value());
    }

    @Test
    void jsonDeserializationFailsForUnregisteredTopicType() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("json-topic", Event.Serialization.JSON);

        var deserializer = new DynamicDeserializer(
                new KafkaProperties(),
                new GenericConversionService(),
                eventRegistry
        );

        var payload = "{}".getBytes(StandardCharsets.UTF_8);

        var exception = assertThrows(IllegalArgumentException.class,
                () -> deserializer.deserialize("json-topic", payload));

        assertEquals("No type registered for topic: json-topic", exception.getMessage());
    }

    @Test
    void avroToEventAppliesReaderSchemaDefaultsToOlderMessages() {
        var eventRegistry = registryWithReaderSchema(EvolvedEvent.class, """
                {"type":"record","name":"DynamicDeserializerTest_EvolvedEvent","namespace":"be.appify.prefab.core.kafka",
                 "fields":[{"name":"value","type":"string"},{"name":"label","type":"string","default":"unknown"}]}""");
        var deserializer = createDeserializer(evolvedConverters(), eventRegistry);

        var event = invokeToEvent(deserializer, recordWithSchema("be.appify.prefab.core.kafka",
                "DynamicDeserializerTest_EvolvedEvent", "v"));

        assertEquals(new EvolvedEvent("v", "unknown"), event);
    }

    @Test
    void avroToEventAppliesFieldAliasesAndTypePromotion() {
        var eventRegistry = registryWithReaderSchema(EvolvedEvent.class, """
                {"type":"record","name":"DynamicDeserializerTest_EvolvedEvent","namespace":"be.appify.prefab.core.kafka",
                 "fields":[{"name":"value","type":"string","aliases":["oldValue"]}]}""");
        var deserializer = createDeserializer(evolvedConverters(), eventRegistry);
        var writer = new Schema.Parser().parse("""
                {"type":"record","name":"DynamicDeserializerTest_EvolvedEvent","namespace":"be.appify.prefab.core.kafka",
                 "fields":[{"name":"oldValue","type":"string"}]}""");
        var written = new GenericData.Record(writer);
        written.put("oldValue", "legacy");

        var event = (EvolvedEvent) invokeToEvent(deserializer, written);

        assertEquals("legacy", event.value());
    }

    @Test
    void avroToEventRoutesRenamedRecordThroughReaderSchemaAlias() {
        var eventRegistry = registryWithReaderSchema(EvolvedEvent.class, """
                {"type":"record","name":"DynamicDeserializerTest_EvolvedEvent","namespace":"be.appify.prefab.core.kafka",
                 "aliases":["legacy.OldEvent"],
                 "fields":[{"name":"value","type":"string"}]}""");
        var deserializer = createDeserializer(evolvedConverters(), eventRegistry);

        var event = (EvolvedEvent) invokeToEvent(deserializer, recordWithSchema("legacy", "OldEvent", "v"));

        assertEquals("v", event.value());
    }

    @Test
    void avroToEventRoutesRenamedRecordThroughPreviousNameAnnotation() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("avro-topic", RenamedAvroEvent.class, Event.Serialization.AVRO);
        var conversionService = new GenericConversionService();
        conversionService.addConverter(GenericRecord.class, RenamedAvroEvent.class,
                source -> new RenamedAvroEvent(source.get("value").toString()));
        var deserializer = createDeserializer(conversionService, eventRegistry);

        assertInstanceOf(RenamedAvroEvent.class, invokeToEvent(deserializer, recordWithSchema("legacy", "OldName", "v")));
        assertInstanceOf(RenamedAvroEvent.class,
                invokeToEvent(deserializer, recordWithSchema("legacy", "Retired", "v")));
    }

    @Test
    void avroToEventConvertsIncompatibleMessageAsWritten() {
        var eventRegistry = registryWithReaderSchema(EvolvedEvent.class, """
                {"type":"record","name":"DynamicDeserializerTest_EvolvedEvent","namespace":"be.appify.prefab.core.kafka",
                 "fields":[{"name":"value","type":"string"},{"name":"required","type":"string"}]}""");
        var deserializer = createDeserializer(evolvedConverters(), eventRegistry);

        var event = (EvolvedEvent) invokeToEvent(deserializer, recordWithSchema("be.appify.prefab.core.kafka",
                "DynamicDeserializerTest_EvolvedEvent", "v"));

        assertEquals("v", event.value());
    }

    private static EventRegistry registryWithReaderSchema(Class<?> type, String schema) {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("avro-topic", type, Event.Serialization.AVRO);
        eventRegistry.registerReaderSchema(type, new Schema.Parser().parse(schema));
        return eventRegistry;
    }

    private static GenericConversionService evolvedConverters() {
        var conversionService = new GenericConversionService();
        conversionService.addConverter(GenericRecord.class, EvolvedEvent.class, source -> new EvolvedEvent(
                source.get("value").toString(),
                source.hasField("label") ? source.get("label").toString() : null));
        return conversionService;
    }
    private static DynamicDeserializer createDeserializer(
            GenericConversionService conversionService,
            EventRegistry eventRegistry
    ) {
        var kafkaProperties = new KafkaProperties();
        KafkaSchemaRegistrySupport.enableMockSchemaRegistry(kafkaProperties);
        return new DynamicDeserializer(kafkaProperties, conversionService, eventRegistry);
    }

    private static GenericRecord recordWithSchema(String namespace, String name, String value) {
        var schema = new Schema.Parser().parse("""
                {
                  "type": "record",
                  "name": "%s",
                  "namespace": "%s",
                  "fields": [
                    {"name": "value", "type": "string"}
                  ]
                }
                """.formatted(name, namespace));

        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("value", value);
        return genericRecord;
    }

    private static Object invokeToEvent(DynamicDeserializer deserializer, GenericRecord genericRecord) {
        try {
            Method toEventMethod = DynamicDeserializer.class.getDeclaredMethod("toEvent", String.class, GenericRecord.class);
            toEventMethod.setAccessible(true);
            return toEventMethod.invoke(deserializer, "avro-topic", genericRecord);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError(e.getTargetException());
        }
    }

    private record AvroEvent(String value) {}

    private record EvolvedEvent(String value, String label) {}

    @PreviousName({"OldName", "legacy.Retired"})
    private record RenamedAvroEvent(String value) {}

    private sealed interface SealedAvroEvents permits ConcreteAvroEvent {}

    private record ConcreteAvroEvent(String value) implements SealedAvroEvents {}
}
