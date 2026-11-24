package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.Migration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnsMessageCodecTest {
    @Event(topic = "users", version = "2.0.0")
    record UserCreated(String fullName) {
    }

    record UserCreatedV1(String name) {
    }

    @EventMigration(from = "1", to = "2")
    static class Migrate implements Migration<UserCreatedV1, UserCreated> {
        @Override
        public UserCreated migrate(UserCreatedV1 old) {
            return new UserCreated(old.name().toUpperCase());
        }
    }

    @Event(topic = "shared", typeArguments = Customer.class)
    @Event(topic = "shared", typeArguments = Product.class)
    public record Changed<T>(String id, T payload) {
    }

    public record Customer(String name) {
    }

    public record Product(String sku) {
    }

    private final EventType<Changed<Customer>> customers = new EventType<>() {
    };
    private final EventType<Changed<Product>> products = new EventType<>() {
    };
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final EventRegistry registry = new EventRegistry();
    private final JsonUtil json = new JsonUtil(mapper);
    private final SnsMessageEncoder encoder = new SnsMessageEncoder(
            new SnsSerializer(json, registry, new DefaultConversionService()));
    private final SqsMessageDecoder decoder = new SqsMessageDecoder(new SqsDeserializer(
            json, registry, new DefaultConversionService(), new EventMigrator(List.of(new Migrate())), mapper));

    @Test
    void roundTripsAnEventThroughTheNotificationEnvelope() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);

        var message = encoder.encode("users", new UserCreated("Ada"));
        var decoded = decoder.decode(request("users", UserCreated.class), envelope(message));

        assertThat(message.subject()).isEqualTo(UserCreated.class.getName());
        assertThat(message.headers()).containsEntry("prefab-event-version", "2.0.0");
        assertThat(decoded).contains(new UserCreated("Ada"));
    }

    @Test
    void migratesAMessageWrittenWithAnOlderVersion() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        var old = new SnsMessageEncoder.SnsMessage("{\"name\":\"ada\"}", UserCreated.class.getName(),
                Map.of("prefab-event-version", "1.0.0"));

        var decoded = decoder.decode(request("users", UserCreated.class), envelope(old));

        assertThat(decoded).contains(new UserCreated("ADA"));
    }

    @Test
    void carriesTheConcreteIdentityOfAGenericEvent() {
        registry.register("shared", customers, Event.Serialization.JSON);
        registry.register("shared", products, Event.Serialization.JSON);
        var event = new Changed<>("id", new Customer("Alice"));

        var message = encoder.encode("shared", new TypedEvent<>(customers, event));
        var body = envelope(message);

        assertThat(message.headers()).containsEntry(EventType.HEADER, customers.id());
        assertThat(decoder.decode(new SqsSubscriptionRequest<>("shared", "q", customers, e -> {
        }), body)).contains(event);
        assertThat(decoder.decode(new SqsSubscriptionRequest<>("shared", "q", products, e -> {
        }), body)).isEmpty();
    }

    @Test
    void rejectsAMessageWithAnUnknownEventType() {
        var unknown = new SnsMessageEncoder.SnsMessage("{}", "com.example.UnknownEvent", Map.of());

        assertThatThrownBy(() -> decoder.decode(request("users", Product.class), envelope(unknown)))
                .isInstanceOf(UnknownEventTypeException.class);
    }

    private <T> SqsSubscriptionRequest<T> request(String topic, Class<T> type) {
        return new SqsSubscriptionRequest<>(topic, "queue", type, event -> {
        });
    }

    private String envelope(SnsMessageEncoder.SnsMessage message) {
        var attributes = new HashMap<String, Map<String, String>>();
        message.headers().forEach((name, value) -> attributes.put(name, Map.of("Type", "String", "Value", value)));
        return json.toJson(Map.of("Message", message.payload(), "Subject", message.subject(),
                "MessageAttributes", attributes));
    }
}
