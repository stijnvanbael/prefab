package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.KafkaSchemaRegistrySupport;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.EventVersions;
import be.appify.prefab.core.versioning.Migration;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaMessageCodecTest {

    @Event(topic = "users", version = "2.0.0")
    public record UserCreated(String fullName) {
    }

    public record UserCreatedV1(String firstName, String lastName) {
    }

    @EventMigration(from = "1", to = "2")
    static class Migrate implements Migration<UserCreatedV1, UserCreated> {
        @Override
        public UserCreated migrate(UserCreatedV1 old) {
            return new UserCreated(old.firstName() + " " + old.lastName());
        }
    }

    @Event(topic = "customers", typeArguments = Customer.class)
    public record Changed<T>(String id, T payload) {
    }

    public record Customer(String name) {
    }

    private final EventType<Changed<Customer>> customers = new EventType<>() {
    };

    private KafkaMessageCodec codec() {
        var registry = new EventRegistry();
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        registry.register("customers", customers, Event.Serialization.JSON);
        var properties = new KafkaProperties();
        KafkaSchemaRegistrySupport.enableMockSchemaRegistry(properties);
        var conversion = new DefaultConversionService();
        return new KafkaMessageCodec(new DynamicSerializer(properties, conversion, registry),
                new DynamicDeserializer(properties, conversion, registry, new EventMigrator(List.of(new Migrate()))),
                registry);
    }

    @Test
    void roundTripsJsonEventsAndStampsVersionHeaders() {
        var codec = codec();
        var event = new UserCreated("Ada Lovelace");

        var message = codec.encode("users", event);

        assertThat(new String(message.payload(), StandardCharsets.UTF_8)).contains("Ada Lovelace");
        assertThat(message.header(EventVersions.EVENT_VERSION_HEADER)).contains("2.0.0");
        assertThat(message.decode()).isEqualTo(event);
    }

    @Test
    void roundTripsGenericEventsWithTheEventTypeHeader() {
        var codec = codec();
        var event = new Changed<>("1", new Customer("Ada"));

        var message = codec.encode("customers", new TypedEvent<>(customers, event));

        assertThat(message.header(EventType.HEADER)).contains(customers.id());
        assertThat(message.decode()).isEqualTo(event);
    }

    @Test
    void claimsGenericBindingsOnlyForTheirPlatform() {
        var codec = codec();

        assertThat(codec.supports(customers)).isTrue();
        assertThat(codec.supports(UserCreated.class)).isTrue();
        assertThat(codec.supports(Customer.class)).isFalse();
    }

    @Test
    void appliesMigrationsToMessagesWrittenWithAnOlderVersion() {
        var codec = codec();
        var old = new EncodedMessage("users", Map.of(EventVersions.EVENT_VERSION_HEADER, "1.0.0"),
                "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}".getBytes(StandardCharsets.UTF_8), codec);

        assertThat(old.decode()).isEqualTo(new UserCreated("Ada Lovelace"));
    }
}
