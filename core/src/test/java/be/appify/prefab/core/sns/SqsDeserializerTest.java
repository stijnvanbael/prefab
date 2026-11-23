package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.core.spring.JsonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.GenericConversionService;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqsDeserializerTest {

    private final JsonUtil jsonUtil = new JsonUtil(JsonMapper.builder().build());
    private final EventRegistry eventRegistry = new EventRegistry();
    private final SqsDeserializer deserializer = new SqsDeserializer(
            jsonUtil, eventRegistry, new GenericConversionService());

    @Test
    void deserializeThrowsUnknownEventTypeExceptionForUnregisteredTypeName() {
        eventRegistry.register("test-topic", KnownEvent.class, Event.Serialization.JSON);

        var snsEnvelope = """
                {"Message":"{\\"field\\":\\"value\\"}", "Subject":"com.example.UnknownEvent"}
                """;

        var exception = assertThrows(UnknownEventTypeException.class,
                () -> deserializer.deserialize("test-topic", snsEnvelope, KnownEvent.class));

        assertEquals("com.example.UnknownEvent", exception.eventTypeName());
    }

    @Test
    void deserializeSucceedsForKnownRegisteredType() {
        eventRegistry.register("test-topic", KnownEvent.class, Event.Serialization.JSON);
        var className = KnownEvent.class.getName();

        var snsEnvelope = """
                {"Message":"{\\"field\\":\\"value\\"}", "Subject":"%s"}
                """.formatted(className);

        var event = deserializer.deserialize("test-topic", snsEnvelope, KnownEvent.class);

        assertEquals("value", event.field());
    }

    @Test
    void deserializeContinuesAfterUnknownEventType() {
        eventRegistry.register("test-topic", KnownEvent.class, Event.Serialization.JSON);

        var unknownEnvelope = """
                {"Message":"{\\"field\\":\\"value\\"}", "Subject":"com.example.UnknownEvent"}
                """;
        var knownEnvelope = """
                {"Message":"{\\"field\\":\\"hello\\"}", "Subject":"%s"}
                """.formatted(KnownEvent.class.getName());

        assertThrows(UnknownEventTypeException.class,
                () -> deserializer.deserialize("test-topic", unknownEnvelope, KnownEvent.class));

        var event = deserializer.deserialize("test-topic", knownEnvelope, KnownEvent.class);
        assertEquals("hello", event.field());
    }

    @Test
    void parseNotificationReadsPayloadSubjectAndAttributesWithoutDeserializing() {
        var snsEnvelope = """
                {"Message":"{\\"field\\":\\"value\\"}", "Subject":"com.example.UnknownEvent",
                 "MessageAttributes":{"prefab-message-id":{"Type":"String","Value":"m-1"}}}
                """;

        var notification = deserializer.parseNotification(snsEnvelope);

        assertEquals("{\"field\":\"value\"}", notification.payload());
        assertEquals("com.example.UnknownEvent", notification.subject());
        assertEquals("m-1", notification.attributes().get("prefab-message-id"));
    }

    @Test
    void parseNotificationReturnsARawBodyAsThePayload() {
        var notification = deserializer.parseNotification("plain body");

        assertEquals("plain body", notification.payload());
        assertEquals(null, notification.subject());
        assertEquals(0, notification.attributes().size());
    }

    private record KnownEvent(String field) {}
}

