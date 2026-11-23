package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.DynamicDeserializer;
import be.appify.prefab.core.kafka.DynamicSerializer;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import be.appify.prefab.test.inmemory.InMemoryTransactionManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryEventBusTest {
    record UserCreated(String id) {
    }

    record UserDeleted(String id) {
    }

    record Unregistered(String id) {
    }

    private final List<Object> received = new ArrayList<>();
    private InMemoryEventBus bus;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        var registry = new EventRegistry();
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        registry.register("deleted-users", UserDeleted.class, Event.Serialization.JSON);
        var properties = new KafkaProperties();
        var conversion = new DefaultConversionService();
        var codec = new KafkaMessageCodec(new DynamicSerializer(properties, conversion, registry),
                new DynamicDeserializer(properties, conversion, registry), registry);
        var transactionManager = new InMemoryTransactionManager();
        var outsideTransaction = new TransactionTemplate(transactionManager);
        outsideTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        bus = new InMemoryEventBus(registry, List.of(codec), outsideTransaction);
        transaction = new TransactionTemplate(transactionManager);
    }

    @Test
    void claimsOnlyRegisteredEventTypes() {
        assertThat(bus.canDispatch(UserCreated.class)).isTrue();
        assertThat(bus.canDispatch(Unregistered.class)).isFalse();
    }

    @Test
    void deliversDecodedCopyToEverySubscriber() {
        var first = new ArrayList<Object>();
        var second = new ArrayList<Object>();
        bus.subscribe("users", "first", message -> first.add(message.decode()));
        bus.subscribe("users", "second", message -> second.add(message.decode()));
        var event = new UserCreated("1");

        bus.dispatch(event);

        assertThat(first).containsExactly(event);
        assertThat(second).containsExactly(event);
        assertThat(first.getFirst()).isNotSameAs(event).isNotSameAs(second.getFirst());
    }

    @Test
    void honoursTopicOverrides() {
        bus.subscribe("users", "default", message -> received.add("default"));
        bus.subscribe("custom", "custom", message -> received.add("custom"));

        bus.dispatch(new UserCreated("1"), "custom");

        assertThat(received).containsExactly("custom");
    }

    @Test
    void failsThePublisherWhenTheEventCannotBeSerialized() {
        assertThatThrownBy(() -> bus.dispatch(new Unregistered("1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void queuesEventsPublishedByHandlersInsteadOfNestingThem() {
        bus.subscribe("users", "creator", message -> {
            received.add("created:start");
            bus.dispatch(new UserDeleted("1"));
            received.add("created:end");
        });
        bus.subscribe("users", "second", message -> received.add("second"));
        bus.subscribe("deleted-users", "deleter", message -> received.add("deleted"));

        bus.dispatch(new UserCreated("1"));

        assertThat(received).containsExactly("created:start", "created:end", "second", "deleted");
    }

    @Test
    void deliversInPublishingOrder() {
        bus.subscribe("users", "all", message -> received.add(message.decode()));
        bus.subscribe("users", "republish", message -> {
            if (message.decode() instanceof UserCreated(var id) && id.equals("1")) {
                bus.dispatch(new UserCreated("3"));
            }
        });

        bus.dispatch(new UserCreated("1"));
        bus.dispatch(new UserCreated("2"));

        assertThat(received).containsExactly(new UserCreated("1"), new UserCreated("3"), new UserCreated("2"));
    }

    @Test
    void deliversAfterCommitOnly() {
        bus.subscribe("users", "all", message -> received.add("delivered"));

        transaction.executeWithoutResult(status -> {
            bus.dispatch(new UserCreated("1"));
            received.add("in transaction");
        });

        assertThat(received).containsExactly("in transaction", "delivered");
    }

    @Test
    void deliversEventsPublishedByTransactionalHandlers() {
        bus.subscribe("users", "transactional-handler", message ->
                transaction.executeWithoutResult(status -> bus.dispatch(new UserDeleted("1"))));
        bus.subscribe("deleted-users", "collector", message -> received.add(message.decode()));

        transaction.executeWithoutResult(status -> bus.dispatch(new UserCreated("1")));

        assertThat(received).containsExactly(new UserDeleted("1"));
    }

    @Test
    void dropsEventsOfARolledBackTransaction() {
        bus.subscribe("users", "all", message -> received.add("delivered"));

        transaction.executeWithoutResult(status -> {
            bus.dispatch(new UserCreated("1"));
            status.setRollbackOnly();
        });

        assertThat(received).isEmpty();
    }

    @Test
    void recordsHandlerFailuresWithoutPropagatingThem() {
        var failure = new IllegalStateException("boom");
        bus.subscribe("users", "broken", message -> {
            throw failure;
        });
        bus.subscribe("users", "healthy", message -> received.add("delivered"));

        bus.dispatch(new UserCreated("1"));

        assertThat(received).containsExactly("delivered");
        assertThat(bus.failures()).singleElement().satisfies(recorded -> {
            assertThat(recorded.subscriber()).isEqualTo("broken");
            assertThat(recorded.topic()).isEqualTo("users");
            assertThat(recorded.cause()).isSameAs(failure);
        });
        assertThat(bus.drainFailures()).hasSize(1);
        assertThat(bus.failures()).isEmpty();
    }

    @Test
    void ignoresUnknownEventTypes() {
        bus.subscribe("users", "unknown", message -> {
            throw new UnknownEventTypeException("com.example.Gone");
        });

        bus.dispatch(new UserCreated("1"));

        assertThat(bus.failures()).isEmpty();
    }

    @Test
    void stopsDeliveringAfterTheSubscriptionIsClosed() {
        var subscription = bus.subscribe("users", "all", message -> received.add("delivered"));
        subscription.close();

        bus.dispatch(new UserCreated("1"));

        assertThat(received).isEmpty();
    }

    @Test
    void resetForgetsRecordedFailures() {
        bus.subscribe("users", "broken", message -> {
            throw new IllegalStateException("boom");
        });
        bus.dispatch(new UserCreated("1"));

        bus.reset();

        assertThat(bus.failures()).isEmpty();
    }
}
