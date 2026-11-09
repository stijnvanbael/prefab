package be.appify.prefab.core.domain;

import java.util.Objects;

/**
 * Publication envelope retaining an event's concrete type until transport serialization.
 * The envelope itself is never part of the serialized payload.
 *
 * @param type concrete event type
 * @param value event payload
 * @param <E> event type
 */
public record TypedEvent<E>(EventType<E> type, E value) {
    /** Validates that the payload belongs to the captured event class. */
    public TypedEvent {
        Objects.requireNonNull(type, "Event type");
        Objects.requireNonNull(value, "Event value");
        if (!type.rawType().isInstance(value)) {
            throw new IllegalArgumentException("Event value does not match " + type);
        }
    }

    /**
     * Unwraps a typed publication, leaving ordinary events unchanged.
     *
     * @param event publication candidate
     * @return the payload
     */
    public static Object payload(Object event) {
        return event instanceof TypedEvent<?> typed ? typed.value() : event;
    }
}
