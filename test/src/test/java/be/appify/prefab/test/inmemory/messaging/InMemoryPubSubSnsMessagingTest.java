package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.pubsub.PubSubSubscriptions;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsSubscriptions;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.Migration;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.TestEventConsumer;
import com.google.pubsub.v1.PubsubMessage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.context.TestContext;
import tools.jackson.databind.json.JsonMapper;

import static be.appify.prefab.core.annotations.Event.Platform.KAFKA;
import static be.appify.prefab.core.annotations.Event.Platform.PUB_SUB;
import static be.appify.prefab.core.annotations.Event.Platform.SNS_SQS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class InMemoryPubSubSnsMessagingTest {

    @Event(topic = "kafka-users", platform = KAFKA)
    public record KafkaUserCreated(String id) {
    }

    @Event(topic = "pubsub-users", platform = PUB_SUB, version = "2.0.0")
    public record PubSubUserCreated(String fullName) {
    }

    public record PubSubUserCreatedV1(String name) {
    }

    @EventMigration(from = "1", to = "2")
    public static class MigratePubSubUser implements Migration<PubSubUserCreatedV1, PubSubUserCreated> {
        @Override
        public PubSubUserCreated migrate(PubSubUserCreatedV1 old) {
            return new PubSubUserCreated(old.name().toUpperCase());
        }
    }

    @Event(topic = "sns-users", platform = SNS_SQS, version = "2.0.0")
    public record SnsUserCreated(String fullName) {
    }

    public record SnsUserCreatedV1(String name) {
    }

    @EventMigration(from = "1", to = "2")
    public static class MigrateSnsUser implements Migration<SnsUserCreatedV1, SnsUserCreated> {
        @Override
        public SnsUserCreated migrate(SnsUserCreatedV1 old) {
            return new SnsUserCreated(old.name().toUpperCase());
        }
    }

    @Event(topic = "pubsub-shared", typeArguments = Customer.class, platform = PUB_SUB)
    @Event(topic = "pubsub-shared", typeArguments = Product.class, platform = PUB_SUB)
    public record PubSubChanged<T>(String id, T payload) {
    }

    @Event(topic = "sns-shared", typeArguments = Customer.class, platform = SNS_SQS)
    @Event(topic = "sns-shared", typeArguments = Product.class, platform = SNS_SQS)
    public record SnsChanged<T>(String id, T payload) {
    }

    public record Customer(String name) {
    }

    public record Product(String sku) {
    }

    private static final EventType<PubSubChanged<Customer>> PUBSUB_CUSTOMERS = new EventType<>() {
    };
    private static final EventType<PubSubChanged<Product>> PUBSUB_PRODUCTS = new EventType<>() {
    };
    private static final EventType<SnsChanged<Customer>> SNS_CUSTOMERS = new EventType<>() {
    };
    private static final EventType<SnsChanged<Product>> SNS_PRODUCTS = new EventType<>() {
    };

    /** Mirrors a generated {@code {Aggregate}PubSubSubscriber}. */
    public static class PubSubSubscriber {
        final List<Object> users = new ArrayList<>();
        final List<Object> customers = new ArrayList<>();
        final List<Object> products = new ArrayList<>();
        final List<PubsubMessage> raw = new ArrayList<>();

        public PubSubSubscriber(PubSubSubscriptions pubSub) {
            pubSub.subscribe(new SubscriptionRequest<>("pubsub-users", "on-user-created",
                    PubSubUserCreated.class, users::add));
            pubSub.subscribe(new SubscriptionRequest<>("pubsub-shared", "on-customer-changed",
                    PUBSUB_CUSTOMERS, customers::add));
            pubSub.subscribe(new SubscriptionRequest<>("pubsub-shared", "on-product-changed",
                    PUBSUB_PRODUCTS, products::add));
            pubSub.subscribe(SubscriptionRequest.raw("pubsub-users", "raw", raw::add));
        }
    }

    /** Mirrors a generated {@code {Aggregate}SqsSubscriber}. */
    public static class SqsSubscriber {
        final List<Object> users = new ArrayList<>();
        final List<Object> customers = new ArrayList<>();
        final List<Object> products = new ArrayList<>();

        public SqsSubscriber(SqsSubscriptions sqs) {
            sqs.subscribe(new SqsSubscriptionRequest<>("sns-users", "on-user-created",
                    SnsUserCreated.class, users::add));
            sqs.subscribe(new SqsSubscriptionRequest<>("sns-shared", "on-customer-changed",
                    SNS_CUSTOMERS, customers::add));
            sqs.subscribe(new SqsSubscriptionRequest<>("sns-shared", "on-product-changed",
                    SNS_PRODUCTS, products::add));
        }
    }

    static class PubSubFixture {
        @TestEventConsumer(topic = "${topics.pubsub}")
        EventConsumer<PubSubUserCreated> consumer;
    }

    static class SnsFixture {
        @TestEventConsumer(topic = "sns-users")
        EventConsumer<SnsUserCreated> consumer;
    }

    static class GenericFixture {
        @TestEventConsumer(topic = "pubsub-shared")
        EventConsumer<PubSubChanged<Customer>> pubsub;

        @TestEventConsumer(topic = "sns-shared")
        EventConsumer<SnsChanged<Customer>> sns;
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InMemoryMessagingAutoConfiguration.class))
            .withBean(EventRegistry.class, () -> {
                var registry = new EventRegistry();
                registry.register("kafka-users", KafkaUserCreated.class, Event.Serialization.JSON);
                registry.register("pubsub-users", PubSubUserCreated.class, Event.Serialization.JSON);
                registry.register("sns-users", SnsUserCreated.class, Event.Serialization.JSON);
                registry.register("pubsub-shared", PUBSUB_CUSTOMERS, Event.Serialization.JSON);
                registry.register("pubsub-shared", PUBSUB_PRODUCTS, Event.Serialization.JSON);
                registry.register("sns-shared", SNS_CUSTOMERS, Event.Serialization.JSON);
                registry.register("sns-shared", SNS_PRODUCTS, Event.Serialization.JSON);
                return registry;
            })
            .withBean(ConversionService.class, DefaultConversionService::new)
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withBean(JsonUtil.class, () -> new JsonUtil(JsonMapper.builder().build()))
            .withBean(EventMigrator.class, () -> new EventMigrator(List.of(new MigratePubSubUser(),
                    new MigrateSnsUser())))
            .withPropertyValues("prefab.test.containers=false", "topics.pubsub=pubsub-users");

    private static TestContext testContext(ApplicationContext context, Object instance) {
        var attributes = new HashMap<String, Object>();
        var testContext = mock(TestContext.class);
        doReturn(context).when(testContext).getApplicationContext();
        doReturn(instance.getClass()).when(testContext).getTestClass();
        doReturn(instance).when(testContext).getTestInstance();
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(testContext).setAttribute(anyString(), any());
        doAnswer(invocation -> attributes.remove(invocation.<String>getArgument(0)))
                .when(testContext).removeAttribute(anyString());
        return testContext;
    }

    @Test
    void everyPlatformClaimsOnlyItsOwnEvents() {
        runner.run(context -> {
            var bus = context.getBean(InMemoryEventBus.class);
            var codecs = context.getBeansOfType(MessageCodec.class).values();

            assertThat(codecs).hasSize(3);
            for (var type : List.of(KafkaUserCreated.class, PubSubUserCreated.class, SnsUserCreated.class)) {
                assertThat(codecs.stream().filter(codec -> codec.supports(type))).hasSize(1);
                assertThat(bus.canDispatch(type)).isTrue();
            }
            for (var type : List.of(PUBSUB_CUSTOMERS, SNS_CUSTOMERS)) {
                assertThat(codecs.stream().filter(codec -> codec.supports(type))).hasSize(1);
                assertThat(bus.canDispatch(type)).isTrue();
            }
            assertThat(context.getBean(PubSubMessageCodec.class).supports(PubSubUserCreated.class)).isTrue();
            assertThat(context.getBean(PubSubMessageCodec.class).supports(SnsUserCreated.class)).isFalse();
            assertThat(context.getBean(SnsMessageCodec.class).supports(SnsUserCreated.class)).isTrue();
            assertThat(context.getBean(SnsMessageCodec.class).supports(KafkaUserCreated.class)).isFalse();
            assertThat(context.getBean(KafkaMessageCodec.class).supports(PubSubUserCreated.class)).isFalse();
        });
    }

    @Test
    void pubSubSubscribersReceiveOnlyPubSubEvents() {
        runner.withBean(PubSubSubscriber.class).run(context -> {
            var bus = context.getBean(InMemoryEventBus.class);
            var subscriber = context.getBean(PubSubSubscriber.class);

            bus.dispatch(new PubSubUserCreated("Ada"));
            bus.dispatch(new SnsUserCreated("ignored"));
            bus.dispatch(new KafkaUserCreated("ignored"));

            assertThat(subscriber.users).containsExactly(new PubSubUserCreated("Ada"));
            assertThat(subscriber.raw).singleElement().satisfies(message -> {
                assertThat(message.getData().toStringUtf8()).isEqualTo("{\"fullName\":\"Ada\"}");
                assertThat(message.getAttributesMap())
                        .containsEntry("type", PubSubUserCreated.class.getName())
                        .containsEntry("prefab-event-version", "2.0.0");
            });
            assertThat(bus.failures()).isEmpty();
        });
    }

    @Test
    void sqsSubscribersReceiveOnlySnsEvents() {
        runner.withBean(SqsSubscriber.class).run(context -> {
            var bus = context.getBean(InMemoryEventBus.class);
            var subscriber = context.getBean(SqsSubscriber.class);

            bus.dispatch(new SnsUserCreated("Ada"));
            bus.dispatch(new PubSubUserCreated("ignored"));

            assertThat(subscriber.users).containsExactly(new SnsUserCreated("Ada"));
            assertThat(bus.failures()).isEmpty();
        });
    }

    @Test
    void genericEventsReachOnlyTheSubscriptionOfTheirConcreteBinding() {
        runner.withBean(PubSubSubscriber.class).withBean(SqsSubscriber.class).run(context -> {
            var bus = context.getBean(InMemoryEventBus.class);
            var pubSub = context.getBean(PubSubSubscriber.class);
            var sqs = context.getBean(SqsSubscriber.class);

            bus.dispatch(new TypedEvent<>(PUBSUB_CUSTOMERS, new PubSubChanged<>("1", new Customer("Ada"))));
            bus.dispatch(new TypedEvent<>(PUBSUB_PRODUCTS, new PubSubChanged<>("2", new Product("sku"))));
            bus.dispatch(new TypedEvent<>(SNS_CUSTOMERS, new SnsChanged<>("3", new Customer("Grace"))));
            bus.dispatch(new TypedEvent<>(SNS_PRODUCTS, new SnsChanged<>("4", new Product("sku2"))));

            assertThat(pubSub.customers).containsExactly(new PubSubChanged<>("1", new Customer("Ada")));
            assertThat(pubSub.products).containsExactly(new PubSubChanged<>("2", new Product("sku")));
            assertThat(sqs.customers).containsExactly(new SnsChanged<>("3", new Customer("Grace")));
            assertThat(sqs.products).containsExactly(new SnsChanged<>("4", new Product("sku2")));
            assertThat(bus.failures()).isEmpty();
        });
    }

    @Test
    void appliesMigrationsToMessagesWrittenWithAnOlderVersion() {
        runner.run(context -> {
            var mapper = context.getBean(JsonMapper.class);
            var pubSub = new EncodedMessage("pubsub-users",
                    Map.of("type", PubSubUserCreated.class.getName(), "prefab-event-version", "1.0.0"),
                    "{\"name\":\"ada\"}".getBytes(StandardCharsets.UTF_8), context.getBean(PubSubMessageCodec.class));
            var envelope = mapper.writeValueAsString(Map.of(
                    "Message", "{\"name\":\"ada\"}",
                    "Subject", SnsUserCreated.class.getName(),
                    "MessageAttributes", Map.of("prefab-event-version", Map.of("Type", "String", "Value", "1.0.0"))));
            var sns = new EncodedMessage("sns-users", Map.of("prefab-event-version", "1.0.0"),
                    envelope.getBytes(StandardCharsets.UTF_8), context.getBean(SnsMessageCodec.class));

            assertThat(pubSub.decode()).isEqualTo(new PubSubUserCreated("ADA"));
            assertThat(sns.decode()).isEqualTo(new SnsUserCreated("ADA"));
        });
    }

    @Test
    void testEventConsumersReceiveEventsOfPubSubAndSnsTopics() {
        runner.run(context -> {
            var listener = new InMemoryEventConsumerExecutionListener();
            var bus = context.getBean(InMemoryEventBus.class);
            var pubSub = new PubSubFixture();
            var sns = new SnsFixture();
            var generic = new GenericFixture();
            var contexts = List.of(testContext(context, pubSub), testContext(context, sns),
                    testContext(context, generic));
            contexts.forEach(listener::beforeTestMethod);

            bus.dispatch(new PubSubUserCreated("Ada"));
            bus.dispatch(new SnsUserCreated("Grace"));
            bus.dispatch(new TypedEvent<>(PUBSUB_CUSTOMERS, new PubSubChanged<>("1", new Customer("Ada"))));
            bus.dispatch(new TypedEvent<>(SNS_CUSTOMERS, new SnsChanged<>("2", new Customer("Grace"))));
            contexts.forEach(listener::afterTestMethod);

            assertThat(pubSub.consumer.messages()).containsExactly(new PubSubUserCreated("Ada"));
            assertThat(sns.consumer.messages()).containsExactly(new SnsUserCreated("Grace"));
            assertThat(generic.pubsub.messages()).containsExactly(new PubSubChanged<>("1", new Customer("Ada")));
            assertThat(generic.sns.messages()).containsExactly(new SnsChanged<>("2", new Customer("Grace")));
        });
    }

    @Test
    void registersNothingInContainersMode() {
        runner.withPropertyValues("prefab.test.containers=true").run(context ->
                assertThat(context).doesNotHaveBean(PubSubMessageCodec.class)
                        .doesNotHaveBean(SnsMessageCodec.class)
                        .doesNotHaveBean(PubSubSubscriptions.class)
                        .doesNotHaveBean(SqsSubscriptions.class));
    }
}
