package be.appify.prefab.streams;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifies a stream backend, e.g. Kafka Streams.
 *
 * <p>Backend modules expose their own constant, for example {@code KafkaStreamBackend.KAFKA}. Backends are compared by
 * name, so a new backend only needs to pick a unique name; no change to the Prefab Streams API is required.
 *
 * @param name the unique, case-insensitive name of the backend
 */
public record StreamBackend(String name) {

    /** Constructs a new StreamBackend. */
    public StreamBackend {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        name = name.strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Creates the identifier of a backend.
     *
     * @param name the unique, case-insensitive name of the backend
     * @return the backend identifier
     */
    public static StreamBackend named(String name) {
        return new StreamBackend(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
