package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PublishTo;
import be.appify.prefab.core.annotations.Versioned;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.versioning.EventVersions;
import be.appify.prefab.core.versioning.UnsupportedEventVersionException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class GenericJsonEventTest {
    @Event(topic = "customers", typeArguments = Customer.class, version = "1.2", minSupportedVersion = "1.1")
    @Event(topic = "products", typeArguments = Product.class, version = "2")
    public record Changed<T>(String id, T payload) {}
    @Versioned(version = "2")
    public record Customer(String name) {}
    public record Product(String sku) {}
    @Event(topic = "customers")
    public record Ordinary(String id) {}

    private final EventType<Changed<Customer>> customers = new EventType<>() {};
    private final EventType<Changed<Product>> products = new EventType<>() {};
    private final EventRegistry registry = registry();

    private EventRegistry registry() {
        var result = new EventRegistry();
        result.register("customers", customers, Event.Serialization.JSON);
        result.register("products", products, Event.Serialization.JSON);
        return result;
    }

    @Test
    void serializesOnlyPayloadAndReadsConcreteRecordArgument() {
        var serializer = new DynamicSerializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var deserializer = new DynamicDeserializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var headers = new RecordHeaders();
        var event = new Changed<>("id", new Customer("Alice"));

        var bytes = serializer.serialize("customers", headers, new TypedEvent<>(customers, event));
        var read = deserializer.deserialize("customers", headers, bytes);

        assertThat(new String(bytes, StandardCharsets.UTF_8)).doesNotContain("rawType", "\"value\"", "\"type\"");
        assertThat(read).isEqualTo(event);
        assertThat(headers.lastHeader(EventType.HEADER).value()).isEqualTo(customers.id().getBytes(StandardCharsets.UTF_8));
        assertThat(headers.lastHeader("prefab-event-version")).isNotNull();
        assertThat(new String(headers.lastHeader(EventVersions.COMPONENT_VERSIONS_HEADER).value(), StandardCharsets.UTF_8))
                .isEqualTo(Customer.class.getName() + "=2.0.0");
    }

    @Test
    void rejectsIdentityRegisteredOnlyOnAnotherTopic() {
        var serializer = new DynamicSerializer(new KafkaProperties(), new DefaultConversionService(), registry);
        assertThatThrownBy(() -> serializer.serialize("products", new RecordHeaders(),
                new TypedEvent<>(customers, new Changed<>("id", new Customer("Alice")))))
                .isInstanceOf(UnknownEventTypeException.class);
    }

    @Test
    void rejectsMissingIdentityRatherThanBindingToRawClass() {
        var deserializer = new DynamicDeserializer(new KafkaProperties(), new DefaultConversionService(), registry);
        assertThatThrownBy(() -> deserializer.deserialize("customers", new RecordHeaders(),
                "{\"id\":\"id\",\"payload\":{\"name\":\"Alice\"}}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("prefab-event-type");
    }

    @Test
    void rejectsUntypedGenericSerialization() {
        var serializer = new DynamicSerializer(new KafkaProperties(), new DefaultConversionService(), registry);

        assertThatThrownBy(() -> serializer.serialize("customers", new RecordHeaders(),
                new Changed<>("c", new Customer("Alice"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("EventType");
    }

    @Test
    void roundTripsTwoBindingsSharingATopic() {
        registry.register("shared", customers, Event.Serialization.JSON);
        registry.register("shared", products, Event.Serialization.JSON);
        var serializer = new DynamicSerializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var deserializer = new DynamicDeserializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var customer = new TypedEvent<>(customers, new Changed<>("c", new Customer("Alice")));
        var product = new TypedEvent<>(products, new Changed<>("p", new Product("sku")));
        for (var event : java.util.List.of(customer, product)) {
            var headers = new RecordHeaders();
            var bytes = serializer.serialize("shared", headers, event);

            assertThat(deserializer.deserialize("shared", headers, bytes)).isEqualTo(event.value());
            assertThat(headers.lastHeader(EventVersions.EVENT_VERSION_HEADER).value())
                    .isEqualTo((event == customer ? "1.2.0" : "2.0.0").getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    void rejectsSpoofedIdentityWithoutLoadingArbitraryClasses() {
        var deserializer = new DynamicDeserializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var headers = new RecordHeaders().add(EventType.HEADER, "java.lang.String".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> deserializer.deserialize("customers", headers, "{}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(UnknownEventTypeException.class);
    }

    @Test
    void enforcesBindingMinimumSupportedVersion() {
        var deserializer = new DynamicDeserializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var headers = new RecordHeaders()
                .add(EventType.HEADER, customers.id().getBytes(StandardCharsets.UTF_8))
                .add(EventVersions.EVENT_VERSION_HEADER, "1.0".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> deserializer.deserialize("customers", headers, "{}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(UnsupportedEventVersionException.class).hasMessageContaining("minSupportedVersion");
    }

    @Test
    void rejectsComponentMajorMismatchInsteadOfBindingAnUnmigratedPayload() {
        var serializer = new DynamicSerializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var deserializer = new DynamicDeserializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var headers = new RecordHeaders();
        var bytes = serializer.serialize("customers", headers,
                new TypedEvent<>(customers, new Changed<>("c", new Customer("Alice"))));
        headers.remove(EventVersions.COMPONENT_VERSIONS_HEADER);

        assertThatThrownBy(() -> deserializer.deserialize("customers", headers, bytes))
                .isInstanceOf(UnsupportedEventVersionException.class).hasMessageContaining("component migration");
    }

    @Test
    void preservesOrdinaryJsonOnATopicAlsoContainingGenericEvents() {
        registry.register("customers", Ordinary.class, Event.Serialization.JSON);
        var serializer = new DynamicSerializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var deserializer = new DynamicDeserializer(new KafkaProperties(), new DefaultConversionService(), registry);
        var headers = new RecordHeaders();
        var event = new Ordinary("id");

        var bytes = serializer.serialize("customers", headers, event);

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("{\"id\":\"id\"}");
        assertThat(headers.lastHeader(EventType.HEADER)).isNull();
        assertThat(deserializer.deserialize("customers", headers, bytes)).isEqualTo(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void producerRoutesOnlyItsBindingAndPreservesFanOutOrderAndKey() {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        registry.register("customer-audit", customers, Event.Serialization.JSON, Changed::id);
        registry.registerPublishTo(customers, PublishTo.ALL);
        when(template.send(anyString(), eq("c"), any())).thenReturn(CompletableFuture.completedFuture(null));
        var producer = new GenericKafkaProducer(template, registry);
        var publication = new TypedEvent<>(customers, new Changed<>("c", new Customer("Alice")));

        producer.dispatch(publication);

        var order = inOrder(template);
        order.verify(template).send("customers", "c", publication);
        order.verify(template).send("customer-audit", "c", publication);
        verifyNoMoreInteractions(template);
    }
}
