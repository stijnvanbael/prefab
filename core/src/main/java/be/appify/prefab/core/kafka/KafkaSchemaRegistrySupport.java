package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

/**
 * Shared schema-registry validation for Kafka AVRO support.
 *
 * <p>Prefab requires a real schema registry whenever an {@link Event.Serialization#AVRO AVRO} Kafka
 * registration is present. Tests that intentionally avoid an external registry may explicitly opt into
 * Confluent's in-memory mock registry via {@value #MOCK_SCHEMA_REGISTRY_ENABLED}.
 */
public final class KafkaSchemaRegistrySupport {

    /** Kafka client property that points Confluent serializers/deserializers at the schema registry. */
    public static final String SCHEMA_REGISTRY_URL = "schema.registry.url";

    /**
     * Kafka client property that explicitly enables the in-memory mock registry.
     *
     * <p>Set it through {@code spring.kafka.properties.prefab.mock-schema-registry.enabled=true} or a
     * producer/consumer/streams-scoped equivalent, and only for tests.
     */
    public static final String MOCK_SCHEMA_REGISTRY_ENABLED = "prefab.mock-schema-registry.enabled";

    static final String MOCK_SCHEMA_REGISTRY_URL = "mock://schema-url";

    private KafkaSchemaRegistrySupport() {
    }

    /**
     * Returns Kafka client properties that are safe to use for AVRO serialization/deserialization.
     *
     * <p>If the registry contains no AVRO topics, the input properties are returned unchanged. When AVRO
     * topics are present, this method requires {@value #SCHEMA_REGISTRY_URL} unless the caller has
     * explicitly opted into the in-memory mock registry via {@value #MOCK_SCHEMA_REGISTRY_ENABLED}.
     *
     * @param kafkaClientProperties the Kafka client properties to validate
     * @param eventRegistry the populated event registry
     * @return validated properties, with the mock URL injected only after explicit opt-in
     */
    public static Map<String, Object> avroClientProperties(
            Map<String, Object> kafkaClientProperties,
            EventRegistry eventRegistry
    ) {
        var properties = new HashMap<>(kafkaClientProperties);
        if (eventRegistry == null || !eventRegistry.hasSerialization(Event.Serialization.AVRO)) {
            return properties;
        }
        if (hasConfiguredSchemaRegistryUrl(properties)) {
            return properties;
        }
        if (mockSchemaRegistryEnabled(properties)) {
            properties.put(SCHEMA_REGISTRY_URL, MOCK_SCHEMA_REGISTRY_URL);
            return properties;
        }
        throw new IllegalStateException("""
                Missing Kafka property '%s' for AVRO event serialization.
                Configure a real schema registry URL before starting the application.
                Affected registrations: %s
                For tests only, explicitly opt into the in-memory mock registry with \
                'spring.kafka.properties.%s=true' (or the producer/consumer/streams-scoped equivalent).
                """.formatted(SCHEMA_REGISTRY_URL, describeAffectedRegistrations(eventRegistry), MOCK_SCHEMA_REGISTRY_ENABLED)
                .replace("\n", " ")
                .replaceAll("\\s{2,}", " ")
                .trim());
    }

    /**
     * Enables the in-memory mock schema registry for test helpers that construct Kafka properties directly.
     *
     * @param kafkaProperties the properties object to update
     */
    public static void enableMockSchemaRegistry(KafkaProperties kafkaProperties) {
        kafkaProperties.getProperties().put(MOCK_SCHEMA_REGISTRY_ENABLED, Boolean.TRUE.toString());
    }

    private static boolean hasConfiguredSchemaRegistryUrl(Map<String, Object> properties) {
        var value = properties.get(SCHEMA_REGISTRY_URL);
        return value != null && !value.toString().isBlank();
    }

    private static boolean mockSchemaRegistryEnabled(Map<String, Object> properties) {
        var value = properties.get(MOCK_SCHEMA_REGISTRY_ENABLED);
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private static String describeAffectedRegistrations(EventRegistry eventRegistry) {
        return eventRegistry.topicsWithSerialization(Event.Serialization.AVRO).stream()
                .sorted()
                .map(topic -> {
                    var registeredTypes = eventRegistry.registeredTypesForTopic(topic).stream()
                            .map(Class::getName)
                            .sorted()
                            .collect(Collectors.joining(", "));
                    if (registeredTypes.isBlank()) {
                        return topic;
                    }
                    return "%s [%s]".formatted(topic, registeredTypes);
                })
                .collect(Collectors.joining("; "));
    }
}
