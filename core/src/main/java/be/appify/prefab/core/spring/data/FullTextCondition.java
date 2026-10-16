package be.appify.prefab.core.spring.data;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A full-text search over one or more text properties, used to complement Query by Example (which cannot express
 * full-text matching). A match on any of the properties counts as a match.
 *
 * @param properties
 *         the names of the text properties to search in
 * @param query
 *         the user supplied search text; the condition is ignored when {@code null} or blank
 * @param fuzzy
 *         whether rows with words similar to the search text also match, to tolerate typos; executors of databases
 *         without fuzzy text search ignore it
 * @param similarityThreshold
 *         the minimum word similarity between 0 (exclusive) and 1 for a fuzzy match
 */
public record FullTextCondition(
        List<String> properties,
        @Nullable String query,
        boolean fuzzy,
        double similarityThreshold
) {
    /** The similarity threshold used when none is given. */
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.3;

    /**
     * Creates an exact full-text condition.
     *
     * @param properties
     *         the names of the text properties to search in
     * @param query
     *         the user supplied search text
     */
    public FullTextCondition(List<String> properties, @Nullable String query) {
        this(properties, query, false, DEFAULT_SIMILARITY_THRESHOLD);
    }

    /** Validates that at least one property is given and makes the property list immutable. */
    public FullTextCondition {
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("A full-text condition needs at least one property");
        }
        if (similarityThreshold <= 0 || similarityThreshold > 1) {
            throw new IllegalArgumentException(
                    "The similarity threshold must be between 0 (exclusive) and 1, but was " + similarityThreshold);
        }
        properties = List.copyOf(properties);
    }

    /**
     * Whether this condition has a search text to match with.
     *
     * @return true if the query is not blank
     */
    public boolean isActive() {
        return query != null && !query.isBlank();
    }
}
