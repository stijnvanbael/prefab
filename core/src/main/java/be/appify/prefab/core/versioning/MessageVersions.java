package be.appify.prefab.core.versioning;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The versions a message was written with.
 *
 * @param event      the version of the event contract
 * @param components the versions of the versioned components nested in the event, by fully-qualified type name
 */
public record MessageVersions(EventVersion event, Map<String, EventVersion> components) {

    /** Creates message versions. */
    public MessageVersions {
        components = Map.copyOf(components);
    }

    /**
     * The versions of a message that carries no version headers.
     *
     * @return {@code 1.0.0} for the event and all components
     */
    public static MessageVersions initial() {
        return new MessageVersions(EventVersion.INITIAL, Map.of());
    }

    /**
     * Reads the versions from the headers (or attributes) of a message.
     *
     * @param header looks up a header value by name, returning {@code null} if absent
     * @return the versions the message was written with
     * @throws UnsupportedEventVersionException if a version header is malformed
     */
    public static MessageVersions from(Function<String, String> header) {
        try {
            var eventVersion = header.apply(EventVersions.EVENT_VERSION_HEADER);
            var event = eventVersion == null || eventVersion.isBlank()
                    ? EventVersion.INITIAL
                    : EventVersion.parse(eventVersion);
            return new MessageVersions(event, parseComponents(header.apply(EventVersions.COMPONENT_VERSIONS_HEADER)));
        } catch (IllegalArgumentException e) {
            throw new UnsupportedEventVersionException("Malformed version header: " + e.getMessage());
        }
    }

    /**
     * Returns the version a component was written with.
     *
     * @param type the fully-qualified component type name
     * @return the version, {@code 1.0.0} if the message does not state it
     */
    EventVersion component(String type) {
        return components.getOrDefault(type, EventVersion.INITIAL);
    }

    private static Map<String, EventVersion> parseComponents(String value) {
        var result = new LinkedHashMap<String, EventVersion>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (var entry : value.split(",")) {
            var parts = entry.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid component versions [%s]".formatted(value));
            }
            result.put(parts[0].strip(), EventVersion.parse(parts[1]));
        }
        return result;
    }
}
