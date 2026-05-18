package be.appify.prefab.core.kafka;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventRegistryTest {

    private EventRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EventRegistry();
    }

    @Test
    @DisplayName("registerType without extractor: keyFor returns empty")
    void registerTypeWithoutExtractor_keyForReturnsEmpty() {
        registry.registerType("my-topic", MyEvent.class);

        assertEquals(Optional.empty(), registry.keyFor(new MyEvent("123")));
    }

    @Test
    @DisplayName("registerType with extractor: keyFor returns extracted key")
    void registerTypeWithExtractor_keyForReturnsKey() {
        registry.registerType("my-topic", MyEvent.class, MyEvent::id);

        assertEquals(Optional.of("abc"), registry.keyFor(new MyEvent("abc")));
    }

    @Test
    @DisplayName("keyFor resolves key extractor via supertype")
    void keyFor_resolvesBySupertype() {
        registry.registerType("my-topic", MySealedEvent.class, MySealedEvent::id);

        assertEquals(Optional.of("xyz"), registry.keyFor(new MySealedEvent.Concrete("xyz")));
    }

    @Test
    @DisplayName("topicForType returns registered topic")
    void topicForType_returnsRegisteredTopic() {
        registry.registerType("my-topic", MyEvent.class);

        assertEquals("my-topic", registry.topicForType(MyEvent.class));
    }

    @Test
    @DisplayName("topicForType throws when no topic registered")
    void topicForType_throwsWhenNotRegistered() {
        assertThrows(IllegalArgumentException.class, () -> registry.topicForType(MyEvent.class));
    }

    @Test
    @DisplayName("resolveType returns registered type for topic")
    void resolveType_returnsTypeForTopic() {
        registry.registerType("my-topic", MyEvent.class);

        var resolved = registry.resolveType("my-topic", new byte[0], null);

        assertEquals(MyEvent.class, resolved.getRawClass());
    }

    @Test
    @DisplayName("resolveType throws when topic not registered")
    void resolveType_throwsWhenTopicNotRegistered() {
        assertThrows(IllegalArgumentException.class,
                () -> registry.resolveType("unknown-topic", new byte[0], null));
    }

    @Test
    @DisplayName("registeredTypesForTopic includes subtypes of sealed interface")
    void registeredTypesForTopic_includesSubtypes() {
        registry.registerType("my-topic", MySealedEvent.class);

        var types = registry.registeredTypesForTopic("my-topic");

        assertTrue(types.contains(MySealedEvent.class));
        assertTrue(types.contains(MySealedEvent.Concrete.class));
    }

    @Test
    @DisplayName("allowedClassNames includes subtypes of sealed interface")
    void allowedClassNames_includesSubtypes() {
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
