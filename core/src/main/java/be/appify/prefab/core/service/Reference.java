package be.appify.prefab.core.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;

/**
 * A reference to another aggregate root.
 *
 * @param <T>
 *         the type of the referenced aggregate root
 */
public interface Reference<T> {
    /**
     * Creates a new reference with a random ID.
     *
     * @param <T>
     *         the type of the referenced aggregate root
     * @return a new reference with a random ID
     */
    static <T> Reference<T> create() {
        return fromId(UUID.randomUUID().toString());
    }

    /**
     * Gets the ID of the referenced aggregate root.
     *
     * @return the ID of the referenced aggregate root
     */
    @JsonValue
    String id();

    /**
     * Creates a reference from the given ID.
     *
     * @param id
     *         the ID of the referenced aggregate root
     * @param <T>
     *         the type of the referenced aggregate root
     * @return a reference to the aggregate root with the given ID
     */
    @JsonCreator
    static <T> Reference<T> fromId(String id) {
        return new SimpleReference<>(id);
    }

    /**
     * A simple implementation of the Reference interface.
     *
     * @param id
     *         the ID of the referenced aggregate root
     * @param <T>
     *         the type of the referenced aggregate root
     */
    record SimpleReference<T>(String id) implements Reference<T> {
    }
}
