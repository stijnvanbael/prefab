package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.Migration;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class PubSubMessageCodecTest {
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
    private final PubSubMessageEncoder encoder = new PubSubMessageEncoder(
            new PubSubSerializer(json, registry, new DefaultConversionService()), registry::keyFor);
    private final PubSubMessageDecoder decoder = new PubSubMessageDecoder(new PubSubDeserializer(
            json, registry, new DefaultConversionService(), new EventMigrator(List.of(new Migrate())), mapper),
            registry);

    @Test
    void roundTripsAnEventWithTypeAndVersionAttributes() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        var received = new ArrayList<UserCreated>();
        var request = new SubscriptionRequest<>("users", "sub", UserCreated.class, received::add);

        var message = encoder.encode("users", new UserCreated("Ada"));
        var decoded = decoder.decode(request, message);

        assertThat(message.getAttributesMap())
                .containsEntry(PubSubMessageEncoder.TYPE_ATTRIBUTE, UserCreated.class.getName())
                .containsEntry("prefab-event-version", "2.0.0");
        assertThat(decoded).contains(new UserCreated("Ada"));
    }

    @Test
    void stampsTheOrderingKeyOfTheEvent() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON, UserCreated::fullName);

        var message = encoder.encode("users", new UserCreated("Ada"));

        assertThat(message.getOrderingKey()).isEqualTo("Ada");
    }

    @Test
    void leavesTheOrderingKeyEmptyWithoutAKeyExtractor() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        var message = new PubSubMessageEncoder(new PubSubSerializer(json, registry, new DefaultConversionService()),
                event -> Optional.empty()).encode("users", new UserCreated("Ada"));

        assertThat(message.getOrderingKey()).isEmpty();
    }

    @Test
    void migratesAMessageWrittenWithAnOlderVersion() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        var old = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8("{\"name\":\"ada\"}"))
                .putAttributes(PubSubMessageEncoder.TYPE_ATTRIBUTE, UserCreated.class.getName())
                .putAttributes("prefab-event-version", "1.0.0")
                .build();

        var decoded = decoder.decode(new SubscriptionRequest<>("users", "sub", UserCreated.class, event -> {
        }), old);

        assertThat(decoded).contains(new UserCreated("ADA"));
    }

    @Test
    void carriesTheConcreteIdentityOfAGenericEvent() {
        registry.register("shared", customers, Event.Serialization.JSON);
        registry.register("shared", products, Event.Serialization.JSON);
        var event = new Changed<>("id", new Customer("Alice"));

        var message = encoder.encode("shared", new TypedEvent<>(customers, event));

        assertThat(message.getAttributesMap()).containsEntry(EventType.HEADER, customers.id());
        assertThat(decoder.decode(new SubscriptionRequest<>("shared", "sub", customers, e -> {
        }), message)).contains(event);
        assertThat(decoder.decode(new SubscriptionRequest<>("shared", "sub", products, e -> {
        }), message)).isEmpty();
    }

    @Test
    void ignoresAMessageWithAnUnknownEventType() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        var unknown = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8("{}"))
                .putAttributes(PubSubMessageEncoder.TYPE_ATTRIBUTE, "com.example.UnknownEvent")
                .build();

        var decoded = decoder.decode(new SubscriptionRequest<>("users", "sub", UserCreated.class, e -> {
        }), unknown);

        assertThat(decoded).isEmpty();
    }

    @Test
    void ignoresAMessageWhoseEventIsNotASubtypeOfTheSubscribedType() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        var message = encoder.encode("users", new UserCreated("Ada"));

        var decoded = decoder.decode(new SubscriptionRequest<>("users", "sub", Customer.class, e -> {
        }), message);

        assertThat(decoded).isEmpty();
    }
}
