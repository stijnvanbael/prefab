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
public record Reference<T>(
        @JsonValue String id
) {
    @JsonCreator
    public Reference {
    }

    /**
     * Creates a new reference with a random ID.
     *
     * @param <T>
     *         the type of the referenced aggregate root
     * @return a new reference with a random ID
     */
    public static <T> Reference<T> create() {
        return fromId(UUID.randomUUID().toString());
    }

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
    public static <T> Reference<T> fromId(String id) {
        return new Reference<>(id);
    }
}
