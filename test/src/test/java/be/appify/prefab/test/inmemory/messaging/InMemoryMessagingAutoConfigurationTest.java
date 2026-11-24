package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.DomainEventDispatcher;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.test.inmemory.InMemoryTransactionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryMessagingAutoConfigurationTest {

    @Event(topic = "users")
    public record UserCreated(String id) {
    }

    @Event(topic = "deleted-users")
    public record UserDeleted(String id) {
    }

    @Event(topic = "customers", typeArguments = Customer.class)
    public record Changed<T>(String id, T payload) {
    }

    public record Customer(String name) {
    }

    private static final EventType<Changed<Customer>> CUSTOMERS = new EventType<>() {
    };

    /** Mirrors a generated {@code {Aggregate}KafkaConsumer}. */
    public static class UserConsumer {
        final List<String> received = new ArrayList<>();

        @KafkaListener(topics = "${topics.users}", groupId = "${spring.application.name:app}.created")
        public void onUserCreated(UserCreated event) {
            received.add("created:" + event.id());
        }

        @KafkaListener(topics = "#{'${topics.deleted}'.toLowerCase()}", groupId = "deleted")
        public void onUserDeleted(UserDeleted event) {
            received.add("deleted:" + event.id());
        }

        @KafkaListener(topics = {"users", "${topics.users}"}, groupId = "audit")
        public void audit(UserCreated event) {
            received.add("audit:" + event.id());
        }

        @KafkaListener(topics = "customers", groupId = "customers")
        public void onCustomerChanged(Changed<Customer> event, @Header(EventType.HEADER) String eventType) {
            received.add("customer:" + event.payload().name() + ":" + eventType);
        }
    }

    /** Mirrors a generated consumer: transactional, and publishing a follow-up event. */
    public static class TransactionalConsumer {
        private final DomainEventDispatcher dispatcher;

        public TransactionalConsumer(DomainEventDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Transactional
        @KafkaListener(topics = "users", groupId = "transactional")
        public void onUserCreated(UserCreated event) {
            dispatcher.dispatch(new UserDeleted(event.id()));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TransactionConfiguration {
        @Bean
        InMemoryTransactionManager transactionManager() {
            return new InMemoryTransactionManager();
        }
    }

    public static class FailingConsumer {
        @KafkaListener(topics = "users", groupId = "failing")
        public void onUserCreated(UserCreated event) {
            throw new IllegalStateException("cannot handle " + event.id());
        }
    }

    private static EventRegistry registry() {
        var registry = new EventRegistry();
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        registry.register("deleted-users", UserDeleted.class, Event.Serialization.JSON);
        registry.register("customers", CUSTOMERS, Event.Serialization.JSON);
        return registry;
    }

    private final ApplicationContextRunner runner = runnerWith(InMemoryMessagingAutoConfigurationTest::registry);

    private static ApplicationContextRunner runnerWith(Supplier<EventRegistry> registry) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InMemoryMessagingAutoConfiguration.class))
                .withBean(EventRegistry.class, registry)
                .withBean(ConversionService.class, DefaultConversionService::new)
                .withPropertyValues("prefab.test.containers=false", "topics.users=users",
                        "topics.deleted=DELETED-USERS");
    }

    @Test
    void registersTheBusAsDispatcherInMemoryMode() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(InMemoryEventBus.class);
            assertThat(context.getBean(DomainEventDispatcher.class)).isSameAs(context.getBean(InMemoryEventBus.class));
        });
    }

    @Test
    void registersNothingInContainersMode() {
        runner.withPropertyValues("prefab.test.containers=true")
                .run(context -> assertThat(context).doesNotHaveBean(InMemoryEventBus.class));
    }

    @Test
    void invokesEachListenerMethodOncePerMessageWithResolvedTopics() {
        runner.withBean(UserConsumer.class).run(context -> {
            var bus = context.getBean(InMemoryEventBus.class);

            bus.dispatch(new UserCreated("1"));
            bus.dispatch(new UserDeleted("2"));

            assertThat(context.getBean(UserConsumer.class).received)
                    .containsExactlyInAnyOrder("created:1", "audit:1", "deleted:2");
        });
    }

    @Test
    void deliversEventsPublishedByTransactionalListeners() {
        runner.withUserConfiguration(TransactionConfiguration.class)
                .withBean(TransactionalConsumer.class)
                .withBean(UserConsumer.class)
                .run(context -> {
                    var bus = context.getBean(InMemoryEventBus.class);
                    var transaction = new TransactionTemplate(context.getBean(InMemoryTransactionManager.class));

                    transaction.executeWithoutResult(status -> bus.dispatch(new UserCreated("1")));

                    assertThat(context.getBean(UserConsumer.class).received).contains("deleted:1");
                    assertThat(bus.failures()).isEmpty();
                });
    }

    @Test
    void passesTheEventTypeHeaderOfGenericBindings() {
        runner.withBean(UserConsumer.class).run(context -> {
            var bus = context.getBean(InMemoryEventBus.class);

            bus.dispatch(new TypedEvent<>(CUSTOMERS, new Changed<>("1", new Customer("Ada"))));

            assertThat(context.getBean(UserConsumer.class).received)
                    .containsExactly("customer:Ada:" + CUSTOMERS.id());
        });
    }

    @Test
    void recordsListenerFailuresWithoutFailingThePublisher() {
        runner.withBean(FailingConsumer.class).run(context -> {
            var bus = context.getBean(InMemoryEventBus.class);

            bus.dispatch(new UserCreated("1"));

            assertThat(bus.failures()).singleElement().satisfies(failure -> {
                assertThat(failure.topic()).isEqualTo("users");
                assertThat(failure.subscriber()).endsWith(".onUserCreated");
                assertThat(failure.cause()).hasMessage("cannot handle 1");
            });
        });
    }

    @Test
    void startsWithAvroTopicsWithoutSchemaRegistryUrl() {
        runnerWith(() -> {
            var registry = new EventRegistry();
            registry.register("users", UserCreated.class, Event.Serialization.AVRO);
            return registry;
        }).run(context -> assertThat(context).hasNotFailed().hasSingleBean(InMemoryEventBus.class));
    }
}
