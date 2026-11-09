package be.appify.prefab.core.domain;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;

/**
 * An immutable, type-safe publication handle suitable for a static aggregate field.
 *
 * <p>Binding captures only the event type. Each publication resolves the currently active
 * {@link DomainEventPublisher}, so a handle can be created before infrastructure starts
 * and reused when test infrastructure replaces the publisher.
 *
 * @param <E> the accepted event type
 */
public final class EventPublisher<E> {
    private final EventType<E> type;

    private EventPublisher(EventType<E> type) {
        this.type = Objects.requireNonNull(type, "Event type");
    }

    /**
     * Binds a non-generic event class.
     *
     * @param type event class
     * @param <E> event type
     * @return the bound handle
     * @throws IllegalArgumentException if the class declares unresolved type parameters
     */
    public static <E> EventPublisher<E> bind(Class<E> type) {
        return bind(EventType.of(type));
    }

    /**
     * Binds a concrete event token, retaining its generic arguments.
     *
     * @param type concrete token, for example {@code new EventType<Changed<Customer>>() {}}
     * @param <E> event type
     * @return the bound handle
     */
    public static <E> EventPublisher<E> bind(EventType<E> type) {
        return new EventPublisher<>(type);
    }

    /**
     * Publishes using the currently registered infrastructure publisher.
     *
     * <p>Non-generic events are passed through unchanged. Generic events retain their
     * identity in an infrastructure envelope until transport serialization.
     *
     * @param event the event payload
     * @throws IllegalStateException if no infrastructure publisher is registered
     */
    public void publish(E event) {
        Objects.requireNonNull(event, "Event value");
        if (!type.rawType().isInstance(event)) {
            throw new IllegalArgumentException("Event value does not match " + type);
        }
        var publisher = DomainEventPublisher.getInstance();
        if (publisher == null) {
            throw new IllegalStateException("No domain event publisher is registered for " + type);
        }
        publisher.publish(type.type() instanceof ParameterizedType ? new TypedEvent<>(type, event) : event);
    }
}
