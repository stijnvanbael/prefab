package be.appify.prefab.test.domain;

import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.domain.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PublishedEventsExtension.class)
class BoundEventPublisherTest {
    private static final EventPublisher<Changed<String>> CHANGES =
            EventPublisher.bind(new EventType<Changed<String>>() {});
    private static final EventPublisher<Created> CREATED = EventPublisher.bind(Created.class);

    record Changed<T>(T payload) {}
    record Created(String name) {}

    @Test
    void capturesGenericPayloadWithoutExposingInfrastructureEnvelope(CapturingDomainEventPublisher publisher) {
        var event = new Changed<>("Alice");

        CHANGES.publish(event);

        assertThat(publisher.publishedEvents()).containsExactly(event);
        assertThat(publisher.publishedEventsOf(Changed.class)).containsExactly(event);
    }

    @Test
    void capturesNonGenericPayloadUnchanged(CapturingDomainEventPublisher publisher) {
        var event = new Created("Alice");

        CREATED.publish(event);

        assertThat(publisher.publishedEventsOf(Created.class)).containsExactly(event);
    }
}
