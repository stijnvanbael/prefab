package be.appify.prefab.core.domain;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventPublisherTest {
    record Created(String name) {}
    record Changed<T>(String id, T payload) {}

    @AfterEach
    void resetPublisher() {
        DomainEventPublisher.reset();
    }

    @Test
    void bindsBeforeInfrastructureStartsAndPublishesOrdinaryPayloadUnchanged() {
        var bound = EventPublisher.bind(Created.class);
        var active = new RecordingPublisher();
        DomainEventPublisher.setInstance(active);
        var event = new Created("Alice");

        bound.publish(event);

        assertThat(active.events).containsExactly(event);
    }

    @Test
    void resolvesCurrentPublisherOnEveryCall() {
        var bound = EventPublisher.bind(Created.class);
        var first = new RecordingPublisher();
        var second = new RecordingPublisher();
        var event = new Created("Alice");
        DomainEventPublisher.setInstance(first);
        bound.publish(event);
        DomainEventPublisher.setInstance(second);
        bound.publish(event);

        assertThat(first.events).containsExactly(event);
        assertThat(second.events).containsExactly(event);
    }

    @Test
    void retainsConcreteGenericIdentityInPublication() {
        var token = new EventType<Changed<String>>() {};
        var bound = EventPublisher.bind(token);
        var active = new RecordingPublisher();
        DomainEventPublisher.setInstance(active);
        var event = new Changed<>("key", "Alice");

        bound.publish(event);

        assertThat(active.events).containsExactly(new TypedEvent<>(token, event));
    }

    @Test
    void classTokenAndClassBindingKeepNonGenericPublicationIdentical() {
        var bound = EventPublisher.bind(new EventType<Created>() {});
        var active = new RecordingPublisher();
        DomainEventPublisher.setInstance(active);
        var event = new Created("Alice");

        bound.publish(event);

        assertThat(active.events).containsExactly(event);
    }

    @Test
    void rejectsRawGenericClassBinding() {
        assertThatThrownBy(() -> EventPublisher.bind(Changed.class))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("non-raw");
    }

    @Test
    void reportsMissingInfrastructureRatherThanDroppingEvent() {
        var bound = EventPublisher.bind(Created.class);
        DomainEventPublisher.reset();

        assertThatThrownBy(() -> bound.publish(new Created("Alice")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("No domain event publisher");
    }

    @Test
    void rejectsNullBindingAndPayload() {
        assertThatThrownBy(() -> EventPublisher.bind((Class<Created>) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> EventPublisher.bind((EventType<Created>) null))
                .isInstanceOf(NullPointerException.class);
        var bound = EventPublisher.bind(Created.class);
        assertThatThrownBy(() -> bound.publish(null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("Event value");
    }

    private static class RecordingPublisher extends DomainEventPublisher {
        private final List<Object> events = new ArrayList<>();

        @Override
        public void publish(Object event) {
            events.add(event);
        }
    }
}
