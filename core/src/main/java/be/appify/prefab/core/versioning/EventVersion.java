package be.appify.prefab.core.versioning;

/**
 * A semantic version of an event or component contract.
 *
 * @param major the major version, changed for breaking changes
 * @param minor the minor version, changed for backward-compatible changes
 * @param patch the patch version, changed when the contract does not change
 */
public record EventVersion(int major, int minor, int patch) implements Comparable<EventVersion> {
    /** The version that is assumed when a message carries no version. */
    public static final EventVersion INITIAL = new EventVersion(1, 0, 0);

    /**
     * Parses a version. Missing minor and patch segments default to 0, so {@code "2"} is {@code 2.0.0}.
     *
     * @param value the version text
     * @return the parsed version
     * @throws IllegalArgumentException if the text is not a version
     */
    public static EventVersion parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Event version must not be blank");
        }
        var segments = value.strip().split("\\.");
        if (segments.length > 3) {
            throw new IllegalArgumentException("Invalid event version [%s]".formatted(value));
        }
        try {
            return new EventVersion(
                    Integer.parseInt(segments[0]),
                    segments.length > 1 ? Integer.parseInt(segments[1]) : 0,
                    segments.length > 2 ? Integer.parseInt(segments[2]) : 0);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid event version [%s]".formatted(value), e);
        }
    }

    @Override
    public int compareTo(EventVersion other) {
        var byMajor = Integer.compare(major, other.major);
        if (byMajor != 0) {
            return byMajor;
        }
        var byMinor = Integer.compare(minor, other.minor);
        return byMinor != 0 ? byMinor : Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
