package be.appify.prefab.core.service;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A reference to another aggregate root.
 *
 * @param <T>
 *         the type of the referenced aggregate root
 */
public interface Reference<T> {
    /**
     * @return the ID of the referenced aggregate root
     */
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

    @JsonCreator
    static <T> Reference<T> fromId(String id) {
        return new SimpleReference<>(id);
    }

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
