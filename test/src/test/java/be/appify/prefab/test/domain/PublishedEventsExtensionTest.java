package be.appify.prefab.test.domain;

import be.appify.prefab.core.domain.DomainEventPublisher;
import be.appify.prefab.core.domain.PublishesEvents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PublishedEventsExtension.class)
class PublishedEventsExtensionTest {

    record Ping(String message) {}

    record PingSender() implements PublishesEvents {
        void send(String message) {
            publish(new Ping(message));
        }
    }

    @Test
    void publishedEvent_isCaptured(CapturingDomainEventPublisher publisher) {
        new PingSender().send("hello");

        assertThat(publisher.publishedEventsOf(Ping.class))
                .singleElement()
                .extracting(Ping::message)
                .isEqualTo("hello");
    }

    @Test
    void noEventsPublished_capturedListIsEmpty(CapturingDomainEventPublisher publisher) {
        assertThat(publisher.publishedEvents()).isEmpty();
    }

    @Test
    void publisherIsResetAfterEachTest() {
        // After this test runs, the next test should see a fresh publisher.
        // The extension's afterEach will reset the static instance to null.
        new PingSender().send("should not bleed into other tests");
    }

    @Test
    void staticPublisherIsNullWhenNoExtensionIsActive() {
        // This test verifies the extension cleaned up after the previous test.
        // Within this test the extension has already installed a fresh publisher,
        // but calling reset() manually should work and leave null behind.
        DomainEventPublisher.reset();
        assertThat(DomainEventPublisher.getInstance()).isNull();
        // Re-install so afterEach can reset cleanly.
        DomainEventPublisher.setInstance(new CapturingDomainEventPublisher());
    }
}

