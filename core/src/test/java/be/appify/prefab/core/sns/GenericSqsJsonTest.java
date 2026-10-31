package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.versioning.EventVersions;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.convert.support.DefaultConversionService;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class GenericSqsJsonTest {
    @Event(topic = "shared", typeArguments = Customer.class)
    @Event(topic = "shared", typeArguments = Product.class)
    public record Changed<T>(String id, T payload) {}
    public record Customer(String name) {}
    public record Product(String sku) {}

    private final EventType<Changed<Customer>> customers = new EventType<>() {};
    private final EventType<Changed<Product>> products = new EventType<>() {};
    private final EventRegistry registry = new EventRegistry();
    private final JsonUtil json = new JsonUtil(JsonMapper.builder().build());
    private final SnsSerializer serializer = new SnsSerializer(json, registry, new DefaultConversionService());
    private final SqsDeserializer deserializer = new SqsDeserializer(json, registry, new DefaultConversionService());

    GenericSqsJsonTest() {
        registry.register("shared", customers, Event.Serialization.JSON);
        registry.register("shared", products, Event.Serialization.JSON);
    }

    @Test
    void roundTripsTheOriginalPayloadAndSkipsTheOtherBinding() {
        var event = new Changed<>("id", new Customer("Alice"));
        var publication = new TypedEvent<>(customers, event);
        var attributes = new HashMap<>(EventVersions.headersFor(publication));
        attributes.put(EventType.HEADER, customers.id());
        var payload = serializer.serialize("shared", publication);
        var envelope = notification(payload, attributes);

        assertThat(payload).isEqualTo("{\"id\":\"id\",\"payload\":{\"name\":\"Alice\"}}");
        assertThat(deserializer.deserializeMatching("shared", envelope, customers)).contains(event);
        assertThat(deserializer.deserializeMatching("shared", envelope, products)).isEmpty();
    }

    @Test
    void rejectsUnknownOrMissingIdentityInsteadOfSkippingIt() {
        assertThatThrownBy(() -> deserializer.deserializeMatching("shared", notification("{}", Map.of()), customers))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(EventType.HEADER);
        assertThatThrownBy(() -> deserializer.deserializeMatching("shared",
                notification("{}", Map.of(EventType.HEADER, "unregistered.Type")), customers))
                .isInstanceOf(UnknownEventTypeException.class);
    }

    @Test
    void rejectsIdentityRegisteredOnlyOnAnotherTopic() {
        registry.register("other", products, Event.Serialization.JSON);
        assertThatThrownBy(() -> deserializer.deserializeMatching("other",
                notification("{}", Map.of(EventType.HEADER, customers.id())), customers))
                .isInstanceOf(UnknownEventTypeException.class);
    }

    @Test
    void rejectsRawGenericSerialization() {
        assertThatThrownBy(() -> serializer.serialize("shared", new Changed<>("id", new Customer("Alice"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("EventType");
    }

    @Test
    void publisherCarriesConcreteIdentityInNotificationHeaders() {
        var template = mock(SnsTemplate.class);
        var util = mock(SqsUtil.class);
        when(util.ensureTopicExists("shared")).thenReturn("arn:shared");
        var publisher = new GenericSnsPublisher(template, util, serializer);
        var event = new Changed<>("id", new Customer("Alice"));
        var notification = ArgumentCaptor.forClass(SnsNotification.class);

        publisher.dispatch(new TypedEvent<>(customers, event), "shared");

        verify(template).sendNotification(eq("arn:shared"), notification.capture());
        assertThat(notification.getValue().getSubject()).isEqualTo(Changed.class.getName());
        assertThat(notification.getValue().getPayload()).isEqualTo(json.toJson(event));
        assertThat(notification.getValue().getHeaders()).containsEntry(EventType.HEADER, customers.id());
    }

    @Test
    void queueConsumptionDeletesNonmatchingBindingsAndDeliversOnlyItsOwnPayload() {
        var client = mock(SqsAsyncClient.class);
        when(client.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));
        var util = new SqsUtil("app", "", 0, 1, 1, 1.0, null, client, deserializer, registry);
        var received = new ArrayList<Changed<Customer>>();
        var request = new SqsSubscriptionRequest<>("shared", "customers", customers, received::add);
        var other = notification("{}", Map.of(EventType.HEADER, products.id()));

        util.processMessage(request, "queue", Message.builder().body(other).receiptHandle("other").build());

        assertThat(received).isEmpty();
        var event = new Changed<>("id", new Customer("Alice"));
        var matching = notification(serializer.serialize("shared", new TypedEvent<>(customers, event)),
                Map.of(EventType.HEADER, customers.id()));
        util.processMessage(request, "queue", Message.builder().body(matching).receiptHandle("matching").build());

        assertThat(received).containsExactly(event);
        verify(client, times(2)).deleteMessage(any(DeleteMessageRequest.class));
    }

    private String notification(String payload, Map<String, String> attributes) {
        var messageAttributes = new HashMap<String, Map<String, String>>();
        attributes.forEach((name, value) -> messageAttributes.put(name, Map.of("Type", "String", "Value", value)));
        return json.toJson(Map.of("Message", payload, "Subject", Changed.class.getName(),
                "MessageAttributes", messageAttributes));
    }
}
