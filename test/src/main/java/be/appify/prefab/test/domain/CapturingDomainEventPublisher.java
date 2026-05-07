package be.appify.prefab.test.domain;

import be.appify.prefab.core.domain.DomainEventPublisher;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DomainEventPublisher} that captures published events for later assertion in unit tests.
 *
 * <p>Intended for use with {@link PublishedEventsExtension}, which installs and uninstalls
 * this publisher around each test method.
 */
public class CapturingDomainEventPublisher extends DomainEventPublisher {

    private final List<Object> capturedEvents = new ArrayList<>();

    @Override
    public void publish(Object event) {
        capturedEvents.add(event);
    }

    /**
     * Returns all events published since the last {@link #clear()} or since this publisher
     * was installed.
     *
     * @return an unmodifiable view of the captured events
     */
    public List<Object> publishedEvents() {
        return List.copyOf(capturedEvents);
    }

    /**
     * Returns all events of the specified type published since this publisher was installed.
     *
     * @param eventType the class of events to filter by
     * @param <T>       the event type
     * @return an unmodifiable list of matching events
     */
    public <T> List<T> publishedEventsOf(Class<T> eventType) {
        return capturedEvents.stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .toList();
    }

    /** Clears all previously captured events. */
    public void clear() {
        capturedEvents.clear();
    }
}

