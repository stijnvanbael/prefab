package be.appify.prefab.core.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A reference to another aggregate root.
 *
 * @param <T> the type of the referenced aggregate root
 */
public interface Reference<T> {
    /**
     * Gets the ID of the referenced aggregate root.
     *
     * @return the ID of the referenced aggregate root
     */
    @JsonValue
    String id();

    /**
     * Checks whether the referenced aggregate root exists.
     *
     * @return true if the referenced aggregate root exists, false otherwise
     */
    boolean exists();

    /**
     * Resolves the reference to the other aggregate root. Any changes to the returned object will not be persisted.
     *
     * @return a read-only version of the referenced aggregate root
     */
    T resolveReadOnly();

    /**
     * Creates a reference from the given ID.
     *
     * @param id the ID of the referenced aggregate root
     * @param <T> the type of the referenced aggregate root
     *
     * @return a reference to the aggregate root with the given ID
     */
    @JsonCreator
    static <T> Reference<T> fromId(String id) {
        return new SimpleReference<>(id);
    }

    /**
     * A simple implementation of the Reference interface.
     *
     * @param id the ID of the referenced aggregate root
     * @param <T> the type of the referenced aggregate root
     */
    record SimpleReference<T>(String id) implements Reference<T> {
        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public T resolveReadOnly() {
            throw new UnsupportedOperationException();
        }
    }
}
