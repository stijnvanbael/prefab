package be.appify.prefab.core.kafka;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventRegistryTest {

    private EventRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EventRegistry();
    }

    @Test
    void keyForReturnsEmptyWhenNoExtractorRegistered() {
        registry.registerType("my-topic", MyEvent.class);

        assertEquals(Optional.empty(), registry.keyFor(new MyEvent("123")));
    }

    @Test
    void keyForReturnsExtractedKey() {
        registry.registerType("my-topic", MyEvent.class, MyEvent::id);

        assertEquals(Optional.of("abc"), registry.keyFor(new MyEvent("abc")));
    }

    @Test
    void keyForResolvesBySupertype() {
        registry.registerType("my-topic", MySealedEvent.class, MySealedEvent::id);

        assertEquals(Optional.of("xyz"), registry.keyFor(new MySealedEvent.Concrete("xyz")));
    }

    @Test
    void topicForTypeReturnsRegisteredTopic() {
        registry.registerType("my-topic", MyEvent.class);

        assertEquals("my-topic", registry.topicForType(MyEvent.class));
    }

    @Test
    void topicForTypeThrowsWhenNotRegistered() {
        assertThrows(IllegalArgumentException.class, () -> registry.topicForType(MyEvent.class));
    }

    @Test
    void typeForReturnsRegisteredClass() {
        registry.registerType("my-topic", MyEvent.class);

        assertEquals(MyEvent.class, registry.typeFor("my-topic"));
    }

    @Test
    void typeForThrowsWhenTopicNotRegistered() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.typeFor("unknown-topic"));
    }

    @Test
    void hasTypeForTopicReturnsTrueWhenRegistered() {
        registry.registerType("my-topic", MyEvent.class);

        assertTrue(registry.hasTypeForTopic("my-topic"));
    }

    @Test
    void hasTypeForTopicReturnsFalseWhenNotRegistered() {
        assertFalse(registry.hasTypeForTopic("unknown-topic"));
    }

    @Test
    void registeredTypesForTopicIncludesSubtypes() {
        registry.registerType("my-topic", MySealedEvent.class);

        var types = registry.registeredTypesForTopic("my-topic");

        assertTrue(types.contains(MySealedEvent.class));
        assertTrue(types.contains(MySealedEvent.Concrete.class));
    }

    @Test
    void allowedClassNamesIncludesSubtypes() {
        registry.registerType("my-topic", MySealedEvent.class);

        var names = registry.allowedClassNames();

        assertTrue(names.contains(MySealedEvent.class.getName()));
        assertTrue(names.contains(MySealedEvent.Concrete.class.getName()));
    }

    private record MyEvent(String id) {}

    private sealed interface MySealedEvent permits MySealedEvent.Concrete {
        String id();

        record Concrete(String id) implements MySealedEvent {}
    }
}
