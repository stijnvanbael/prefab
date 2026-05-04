package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.util.SerializationRegistry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
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

        var typeResolver = new KafkaJsonTypeResolver();
        typeResolver.registerType("avro-topic", AvroEvent.class);
        var deserializer = createDeserializer(conversionService, typeResolver);
        var genericRecord = recordWithSchema("different.namespace", "DynamicDeserializerTest_AvroEvent", "allowed");

        var event = invokeToEvent(deserializer, genericRecord);

        assertInstanceOf(AvroEvent.class, event);
        assertEquals("allowed", ((AvroEvent) event).value());
    }

    @Test
    void avroToEventResolvesSealedInterfaceSubtypeBySchemaName() {
        var conversionService = new GenericConversionService();
        conversionService.addConverter(GenericRecord.class, SealedEvents.SubEvent.class,
                source -> new SealedEvents.SubEvent(source.get("value").toString()));

        var typeResolver = new KafkaJsonTypeResolver();
        typeResolver.registerType("avro-topic", SealedEvents.class);
        var deserializer = createDeserializer(conversionService, typeResolver);
        var genericRecord = recordWithSchema("different.namespace", "DynamicDeserializerTest_SealedEvents_SubEvent", "hello");

        var event = invokeToEvent(deserializer, genericRecord);

        assertInstanceOf(SealedEvents.SubEvent.class, event);
        assertEquals("hello", ((SealedEvents.SubEvent) event).value());
    }

    @Test
    void avroToEventFailsWhenTargetClassCannotBeResolved() {
        var typeResolver = new KafkaJsonTypeResolver();
        typeResolver.registerType("avro-topic", AvroEvent.class);
        var deserializer = createDeserializer(new GenericConversionService(), typeResolver);
        var genericRecord = recordWithSchema("be.appify.prefab.core.kafka", "UnknownAvroEvent", "value");

        var exception = assertThrows(NoSuchElementException.class, () -> invokeToEvent(deserializer, genericRecord));

        assertEquals("be.appify.prefab.core.kafka.UnknownAvroEvent", exception.getMessage());
    }

    @Test
    void avroToEventFailsWhenConverterIsMissing() {
        var typeResolver = new KafkaJsonTypeResolver();
        typeResolver.registerType("avro-topic", AvroEvent.class);
        var deserializer = createDeserializer(new GenericConversionService(), typeResolver);
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

        var typeResolver = new KafkaJsonTypeResolver();
        typeResolver.registerType("avro-topic", SealedAvroEvents.class);
        var deserializer = createDeserializer(conversionService, typeResolver);
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
        var registry = new SerializationRegistry();
        registry.register("json-topic", Event.Serialization.JSON);

        var deserializer = new DynamicDeserializer(
                new KafkaProperties(),
                new GenericConversionService(),
                registry,
                new KafkaJsonTypeResolver()
        );

        var payload = "{}".getBytes(StandardCharsets.UTF_8);

        var exception = assertThrows(IllegalArgumentException.class,
                () -> deserializer.deserialize("json-topic", payload));

        assertEquals("No type registered for topic: json-topic", exception.getMessage());
    }

    private static DynamicDeserializer createDeserializer(
            GenericConversionService conversionService,
            KafkaJsonTypeResolver typeResolver
    ) {
        var registry = new SerializationRegistry();
        registry.register("avro-topic", Event.Serialization.AVRO);
        return new DynamicDeserializer(new KafkaProperties(), conversionService, registry, typeResolver);
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

    private sealed interface SealedAvroEvents permits ConcreteAvroEvent {}

    private record ConcreteAvroEvent(String value) implements SealedAvroEvents {}
}

