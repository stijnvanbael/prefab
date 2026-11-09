package be.appify.prefab.core.spring;

import be.appify.prefab.core.domain.DomainEventDispatcher;
import be.appify.prefab.core.domain.DomainEventPublisher;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpringDomainEventPublisherTest {

    private RecordingEventPublisher applicationEventPublisher;
    private TrackingDispatcher dispatcher;
    private SpringDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        applicationEventPublisher = new RecordingEventPublisher();
        dispatcher = new TrackingDispatcher(false);
        publisher = new SpringDomainEventPublisher(applicationEventPublisher, List.of(dispatcher));
    }

    @AfterEach
    void tearDown() {
        DomainEventPublisher.reset();
    }

    @Test
    @DisplayName("publish_givenRegisteredEventType_dispatchesDirectlyAndSkipsSpringBus")
    void publish_givenRegisteredEventType_dispatchesDirectlyAndSkipsSpringBus() {
        var event = new RegisteredEvent("data");
        dispatcher = new TrackingDispatcher(true);
        publisher = new SpringDomainEventPublisher(applicationEventPublisher, List.of(dispatcher));

        publisher.publish(event);

        assertEquals(List.of(event), dispatcher.received);
        assertEquals(List.of(), applicationEventPublisher.received);
    }

    @Test
    @DisplayName("publish_givenUnregisteredEventType_fallsBackToApplicationEventPublisher")
    void publish_givenUnregisteredEventType_fallsBackToApplicationEventPublisher() {
        var event = new UnregisteredEvent("data");
        dispatcher = new TrackingDispatcher(false);
        publisher = new SpringDomainEventPublisher(applicationEventPublisher, List.of(dispatcher));

        publisher.publish(event);

        assertEquals(List.of(event), applicationEventPublisher.received);
        assertEquals(List.of(), dispatcher.received);
    }

    @Test
    @DisplayName("publish_givenMultipleMatchingDispatchers_allReceiveTheEvent")
    void publish_givenMultipleMatchingDispatchers_allReceiveTheEvent() {
        var event = new RegisteredEvent("data");
        var first = new TrackingDispatcher(true);
        var second = new TrackingDispatcher(true);
        publisher = new SpringDomainEventPublisher(applicationEventPublisher, List.of(first, second));

        publisher.publish(event);

        assertEquals(List.of(event), first.received);
        assertEquals(List.of(event), second.received);
        assertEquals(List.of(), applicationEventPublisher.received);
    }

    @Test
    @DisplayName("publish_givenNoDispatchers_allEventsFallBackToApplicationEventPublisher")
    void publish_givenNoDispatchers_allEventsFallBackToApplicationEventPublisher() {
        publisher = new SpringDomainEventPublisher(applicationEventPublisher, List.of());
        var event = new UnregisteredEvent("data");

        publisher.publish(event);

        assertEquals(List.of(event), applicationEventPublisher.received);
    }

    @Test
    @DisplayName("constructor_registersInstanceOnDomainEventPublisher")
    void constructor_registersInstanceOnDomainEventPublisher() {
        assertSame(publisher, DomainEventPublisher.getInstance());
    }

    @Test
    @DisplayName("unregister_clearsInstanceFromDomainEventPublisher")
    void unregister_clearsInstanceFromDomainEventPublisher() {
        publisher.unregister();

        assertNull(DomainEventPublisher.getInstance());
    }

    @Test
    void boundGenericPublisher_dispatchesUsingPayloadClassAndRetainsIdentity() {
        var type = new EventType<GenericEvent<String>>() {};
        var bound = EventPublisher.bind(type);
        var event = new GenericEvent<>("data");
        var received = new ArrayList<Object>();
        var typedDispatcher = new DomainEventDispatcher() {
            @Override
            public boolean canDispatch(Class<?> eventType) {
                return eventType == GenericEvent.class;
            }

            @Override
            public void dispatch(Object publication, String... topicOverrides) {
                received.add(publication);
            }
        };
        publisher = new SpringDomainEventPublisher(applicationEventPublisher, List.of(typedDispatcher));

        bound.publish(event);

        assertEquals(List.of(new TypedEvent<>(type, event)), received);
        assertEquals(List.of(), applicationEventPublisher.received);
    }

    @Test
    void boundGenericPublisher_withoutDispatcherFailsRatherThanPublishingEnvelopeOnSpringBus() {
        var bound = EventPublisher.bind(new EventType<GenericEvent<String>>() {});

        assertThrows(IllegalArgumentException.class, () -> bound.publish(new GenericEvent<>("data")));

        assertEquals(List.of(), applicationEventPublisher.received);
    }

    // --- test doubles ---

    private static final class RecordingEventPublisher implements ApplicationEventPublisher {
        final List<Object> received = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            received.add(event);
        }
    }

    private static final class TrackingDispatcher implements DomainEventDispatcher {
        final List<Object> received = new ArrayList<>();
        private final boolean accepts;

        TrackingDispatcher(boolean accepts) {
            this.accepts = accepts;
        }

        @Override
        public boolean canDispatch(Class<?> eventType) {
            return accepts;
        }

        @Override
        public void dispatch(Object event, String... topicOverrides) {
            received.add(event);
        }
    }

    private record RegisteredEvent(String data) {}
    private record UnregisteredEvent(String data) {}
    private record GenericEvent<T>(T data) {}
}
