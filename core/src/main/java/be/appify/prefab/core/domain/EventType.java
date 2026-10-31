package be.appify.prefab.core.domain;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * Captures a concrete event type, including its generic arguments.
 *
 * <p>Declare a token as {@code new EventType<Changed<Customer>>() {}}. Tokens contain no
 * transport-specific state and compare by their complete type, not their anonymous subclass.
 *
 * @param <E> the event type
 */
public abstract class EventType<E> {
    /** Transport metadata key carrying the canonical concrete event identity. */
    public static final String HEADER = "prefab-event-type";

    private final Type type;

    /** Captures the concrete type argument of a directly extending anonymous subclass. */
    protected EventType() {
        if (!(getClass().getGenericSuperclass() instanceof ParameterizedType parent)
                || parent.getRawType() != EventType.class) {
            throw new IllegalArgumentException("EventType must directly capture a concrete event type");
        }
        type = parent.getActualTypeArguments()[0];
        validate(type);
    }

    private EventType(Type type) {
        validate(type);
        this.type = type;
    }

    /**
     * Creates a token for a non-generic event.
     *
     * @param type event class
     * @param <E> event type
     * @return the concrete token
     */
    public static <E> EventType<E> of(Class<E> type) {
        return new EventType<E>(type) {};
    }

    /**
     * Creates a token from already captured reflective type information.
     * @param type concrete reflective type
     * @param <E> payload type represented by the reflective type
     * @return validated concrete token
     */
    public static <E> EventType<E> from(Type type) {
        return new EventType<E>(type) {};
    }

    /** @return the captured reflective type */
    public final Type type() {
        return type;
    }

    /** @return the erased event class */
    public final Class<?> rawType() {
        return type instanceof ParameterizedType parameterized
                ? (Class<?>) parameterized.getRawType() : (Class<?>) type;
    }

    /** @return a stable identity including fully qualified type arguments */
    public final String id() {
        return type.getTypeName().replace(", ", ",");
    }

    private static void validate(Type type) {
        Objects.requireNonNull(type, "Event type");
        if (type instanceof Class<?> clazz) {
            if (clazz.isPrimitive() || clazz.isArray() || clazz.getTypeParameters().length != 0) {
                throw new IllegalArgumentException("Expected a concrete non-raw event type, got " + type);
            }
        } else if (type instanceof ParameterizedType parameterized) {
            if (!(parameterized.getRawType() instanceof Class<?>)) {
                throw new IllegalArgumentException("Unsupported event type: " + type);
            }
            Arrays.stream(parameterized.getActualTypeArguments()).forEach(EventType::validate);
            if (parameterized.getOwnerType() instanceof ParameterizedType) {
                validate(parameterized.getOwnerType());
            }
        } else {
            throw new IllegalArgumentException("Event type arguments must be concrete, got " + type);
        }
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof EventType<?> token && type.equals(token.type);
    }

    @Override
    public final int hashCode() {
        return type.hashCode();
    }

    @Override
    public final String toString() {
        return id();
    }
}
