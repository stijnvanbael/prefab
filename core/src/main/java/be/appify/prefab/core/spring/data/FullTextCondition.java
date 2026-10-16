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
 */
public record FullTextCondition(List<String> properties, @Nullable String query) {

    /** Validates that at least one property is given and makes the property list immutable. */
    public FullTextCondition {
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("A full-text condition needs at least one property");
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
