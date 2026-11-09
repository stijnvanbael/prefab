package be.appify.prefab.core.pubsub;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.versioning.EventVersions;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.Subscription;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class GenericPubSubJsonTest {
    @Event(topic = "shared", typeArguments = Customer.class)
    @Event(topic = "shared", typeArguments = Product.class)
    public record Changed<T>(String id, T payload) {}
    public record Customer(String name) {}
    public record Product(String sku) {}

    private final EventType<Changed<Customer>> customers = new EventType<>() {};
    private final EventType<Changed<Product>> products = new EventType<>() {};
    private final EventRegistry registry = new EventRegistry();
    private final JsonUtil json = new JsonUtil(JsonMapper.builder().build());
    private final PubSubSerializer serializer =
            new PubSubSerializer(json, registry, new DefaultConversionService());
    private final PubSubDeserializer deserializer =
            new PubSubDeserializer(json, registry, new DefaultConversionService());

    GenericPubSubJsonTest() {
        registry.register("shared", customers, Event.Serialization.JSON);
        registry.register("shared", products, Event.Serialization.JSON);
    }

    @Test
    void roundTripsConcretePayloadWithoutAnEnvelope() {
        var event = new Changed<>("id", new Customer("Alice"));
        var publication = new TypedEvent<>(customers, event);
        var attributes = new HashMap<>(EventVersions.headersFor(publication));
        attributes.put(EventType.HEADER, customers.id());

        var bytes = serializer.serialize("shared", publication);
        var result = deserializer.deserialize("shared", ByteString.copyFrom(bytes), customers, attributes);

        assertThat(new String(bytes, StandardCharsets.UTF_8))
                .isEqualTo("{\"id\":\"id\",\"payload\":{\"name\":\"Alice\"}}");
        assertThat(result).isEqualTo(event);
    }

    @Test
    void rejectsMissingOrWrongConcreteIdentity() {
        var bytes = ByteString.copyFromUtf8("{}");

        assertThatThrownBy(() -> deserializer.deserialize("shared", bytes, customers, Map.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(EventType.HEADER);
        assertThatThrownBy(() -> deserializer.deserialize("shared", bytes, customers,
                Map.of(EventType.HEADER, "unregistered.Type")))
                .isInstanceOf(UnknownEventTypeException.class);
        assertThatThrownBy(() -> deserializer.deserialize("shared", bytes, customers,
                Map.of(EventType.HEADER, products.id())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("binding");
    }

    @Test
    void rejectsRawGenericSerialization() {
        assertThatThrownBy(() -> serializer.serialize("shared", new Changed<>("id", new Customer("Alice"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("EventType");
    }

    @Test
    @SuppressWarnings("unchecked")
    void subscriptionAcknowledgesAnotherBindingWithoutCallingTheHandler() {
        var admin = mock(PubSubAdmin.class);
        var subscriber = mock(PubSubSubscriberTemplate.class);
        when(admin.getTopic(anyString())).thenReturn(com.google.pubsub.v1.Topic.getDefaultInstance());
        when(admin.getSubscription(anyString())).thenReturn(Subscription.getDefaultInstance());
        var util = new PubSubUtil("project", "app", "", 0, 1, 1, 1.0f,
                admin, subscriber, deserializer, registry);
        var received = new ArrayList<Changed<Customer>>();
        util.subscribe(new SubscriptionRequest<>("shared", "customers", customers, received::add));
        ArgumentCaptor<Consumer<BasicAcknowledgeablePubsubMessage>> callback =
                ArgumentCaptor.forClass(Consumer.class);
        verify(subscriber).subscribe(anyString(), callback.capture());
        var message = mock(BasicAcknowledgeablePubsubMessage.class);
        when(message.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8("{}"))
                .putAttributes(EventType.HEADER, products.id()).build());

        callback.getValue().accept(message);

        assertThat(received).isEmpty();
        verify(message).ack();
        verify(message, never()).nack();

        var event = new Changed<>("id", new Customer("Alice"));
        when(message.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(serializer.serialize("shared", new TypedEvent<>(customers, event))))
                .putAttributes(EventType.HEADER, customers.id()).build());
        callback.getValue().accept(message);

        assertThat(received).containsExactly(event);
        verify(message, times(2)).ack();
    }

    @Test
    void publisherCarriesIdentityAndOriginalTypeAsAttributes() {
        var template = mock(PubSubTemplate.class);
        var util = mock(PubSubUtil.class);
        when(util.ensureTopicExists("shared")).thenReturn("projects/project/topics/shared");
        when(template.publish(anyString(), any(PubsubMessage.class)))
                .thenReturn(CompletableFuture.completedFuture("id"));
        var publisher = new GenericPubSubPublisher(template, util, serializer);
        var event = new TypedEvent<>(customers, new Changed<>("id", new Customer("Alice")));
        var message = ArgumentCaptor.forClass(PubsubMessage.class);

        publisher.dispatch(event, "shared");

        verify(template).publish(eq("projects/project/topics/shared"), message.capture());
        assertThat(message.getValue().getAttributesMap()).containsEntry(EventType.HEADER, customers.id())
                .containsEntry("type", Changed.class.getName())
                .containsEntry(EventVersions.EVENT_VERSION_HEADER, "1.0.0");
        assertThat(message.getValue().getData().toStringUtf8())
                .isEqualTo("{\"id\":\"id\",\"payload\":{\"name\":\"Alice\"}}");
    }
}
