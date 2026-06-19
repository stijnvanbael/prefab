package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JSON-based key serialization/deserialization.
 */
@DisplayName("JSON Key Serialization")
class JsonKeySerdeTest {
    private JsonMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = JsonMapper.builder().findAndAddModules().build();
    }

    @Test
    @DisplayName("should serialize and deserialize a simple single-field key")
    void testSimpleSingleFieldKey() {
        var serde = new JsonKeySerde<>(SimpleKey.class, mapper);
        var original = new SimpleKey("test-123");

        byte[] serialized = serde.serializer().serialize("test-topic", original);
        SimpleKey deserialized = serde.deserializer().deserialize("test-topic", serialized);

        assertThat(deserialized).isEqualTo(original);
    }

    @Test
    @DisplayName("should handle multi-field record keys with nested types")
    void testMultiFieldKeyWithNestedTypes() {
        var serde = new JsonKeySerde<>(ComplexKey.class, mapper);
        var original = new ComplexKey(
                new NestedValue("field-456"),
                "string-value",
                Instant.parse("2026-06-19T10:30:00Z")
        );

        byte[] serialized = serde.serializer().serialize("test-topic", original);
        ComplexKey deserialized = serde.deserializer().deserialize("test-topic", serialized);

        assertThat(deserialized)
                .isEqualTo(original)
                .extracting(ComplexKey::nested, ComplexKey::name, ComplexKey::timestamp)
                .containsExactly(
                        new NestedValue("field-456"),
                        "string-value",
                        Instant.parse("2026-06-19T10:30:00Z")
                );
    }

    @Test
    @DisplayName("should handle null values gracefully")
    void testNullSerialization() {
        var serde = new JsonKeySerde<>(SimpleKey.class, mapper);

        byte[] serialized = serde.serializer().serialize("test-topic", null);
        SimpleKey deserialized = serde.deserializer().deserialize("test-topic", serialized);

        assertThat(serialized).isNull();
        assertThat(deserialized).isNull();
    }

    @Test
    @DisplayName("should throw exception on malformed JSON during deserialization")
    void testMalformedJsonThrows() {
        var serde = new JsonKeySerde<>(SimpleKey.class, mapper);
        byte[] malformed = "{invalid json}".getBytes();

        assertThatThrownBy(() -> serde.deserializer().deserialize("test-topic", malformed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to deserialize key")
                .hasMessageContaining("SimpleKey");
    }

    @Test
    @DisplayName("deferred serde should capture key type on first serialization")
    void testDeferredKeySerdeCapture() {
        var deferred = new DeferredJsonKeySerde<SimpleKey>(mapper);

        SimpleKey key1 = new SimpleKey("first");
        SimpleKey key2 = new SimpleKey("second");

        byte[] serialized1 = deferred.serializer().serialize("test-topic", key1);
        byte[] serialized2 = deferred.serializer().serialize("test-topic", key2);

        SimpleKey deserialized1 = deferred.deserializer().deserialize("test-topic", serialized1);
        SimpleKey deserialized2 = deferred.deserializer().deserialize("test-topic", serialized2);

        assertThat(deserialized1).isEqualTo(key1);
        assertThat(deserialized2).isEqualTo(key2);
    }

    @Test
    @DisplayName("deferred serde should fail if deserialized before serialized")
    void testDeferredKeySerdeInvalidStateOnEarlyDeserialize() {
        var deferred = new DeferredJsonKeySerde<SimpleKey>(mapper);

        assertThatThrownBy(() -> deferred.deserializer().deserialize("test-topic", "{\"id\":\"test\"}".getBytes()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Key type not yet resolved");
    }

    // Test key classes
    record SimpleKey(String id) implements Key<SimpleKey> {
    }

    record NestedValue(String code) {
    }

    record ComplexKey(NestedValue nested, String name, Instant timestamp) implements Key<ComplexKey> {
    }
}

