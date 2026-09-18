package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaSchemaRegistrySupportTest {

    @Test
    void avroClientPropertiesFailsFastWithoutSchemaRegistryUrl() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("orders", OrderCreated.class, Event.Serialization.AVRO);
        eventRegistry.register("audit", Event.Serialization.AVRO);

        assertThatThrownBy(() -> KafkaSchemaRegistrySupport.avroClientProperties(Map.of(), eventRegistry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("schema.registry.url")
                .hasMessageContaining("orders")
                .hasMessageContaining(OrderCreated.class.getName())
                .hasMessageContaining("audit")
                .hasMessageContaining(KafkaSchemaRegistrySupport.MOCK_SCHEMA_REGISTRY_ENABLED);
    }

    @Test
    void avroClientPropertiesUsesMockRegistryOnlyAfterExplicitOptIn() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("orders", OrderCreated.class, Event.Serialization.AVRO);

        var properties = KafkaSchemaRegistrySupport.avroClientProperties(
                Map.of(KafkaSchemaRegistrySupport.MOCK_SCHEMA_REGISTRY_ENABLED, "true"),
                eventRegistry);

        assertThat(properties).containsEntry(
                KafkaSchemaRegistrySupport.SCHEMA_REGISTRY_URL,
                "mock://schema-url");
    }

    @Test
    void avroClientPropertiesLeavesJsonOnlyConfigurationUntouched() {
        var eventRegistry = new EventRegistry();
        eventRegistry.register("orders", OrderCreated.class, Event.Serialization.JSON);

        var properties = KafkaSchemaRegistrySupport.avroClientProperties(Map.of(), eventRegistry);

        assertThat(properties).isEmpty();
    }

    private record OrderCreated(String id) {
    }
}
