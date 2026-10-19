package be.appify.prefab.core.spring.data;

import be.appify.prefab.core.annotations.rest.Filter.Weight;
import be.appify.prefab.core.util.TypeaheadQuery;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
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
 * @param weights
 *         the relevance weight per property; properties without an entry have the default weight {@link Weight#D}
 * @param typeahead
 *         whether the last word of the search text also matches as a prefix, so that a partially typed word finds
 *         its completions
 */
public record FullTextCondition(
        List<String> properties,
        @Nullable String query,
        boolean fuzzy,
        double similarityThreshold,
        Map<String, Weight> weights,
        boolean typeahead
) {
    /** The sort property that orders results by relevance, always from most to least relevant. */
    public static final String RELEVANCE = "relevance";

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

    /**
     * Creates a full-text condition without property weights.
     *
     * @param properties
     *         the names of the text properties to search in
     * @param query
     *         the user supplied search text
     * @param fuzzy
     *         whether similar words also match
     * @param similarityThreshold
     *         the minimum word similarity for a fuzzy match
     */
    public FullTextCondition(
            List<String> properties,
            @Nullable String query,
            boolean fuzzy,
            double similarityThreshold
    ) {
        this(properties, query, fuzzy, similarityThreshold, Map.of());
    }

    /**
     * Creates a full-text condition that matches whole words only.
     *
     * @param properties
     *         the names of the text properties to search in
     * @param query
     *         the user supplied search text
     * @param fuzzy
     *         whether similar words also match
     * @param similarityThreshold
     *         the minimum word similarity for a fuzzy match
     * @param weights
     *         the relevance weight per property
     */
    public FullTextCondition(
            List<String> properties,
            @Nullable String query,
            boolean fuzzy,
            double similarityThreshold,
            Map<String, Weight> weights
    ) {
        this(properties, query, fuzzy, similarityThreshold, weights, false);
    }

    /**
     * Returns a copy of this condition that also matches the last word of the search text as a prefix.
     *
     * @return the type-ahead condition
     */
    public FullTextCondition withTypeahead() {
        return new FullTextCondition(properties, query, fuzzy, similarityThreshold, weights, true);
    }

    /**
     * The search text split in the words to match as prefix and the part to match as web search syntax, when this is
     * a type-ahead search.
     *
     * @return the split search text; without prefixes when type-ahead is off
     */
    public TypeaheadQuery typeaheadQuery() {
        var text = query == null ? "" : query;
        return typeahead ? TypeaheadQuery.of(text) : new TypeaheadQuery(text, List.of());
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
        weights = Map.copyOf(weights);
    }

    /**
     * Whether this condition has a search text to match with.
     *
     * @return true if the query is not blank
     */
    public boolean isActive() {
        return query != null && !query.isBlank();
    }

    /**
     * The relevance weight of a property.
     *
     * @param property
     *         the property name
     * @return the configured weight, or {@link Weight#D} when none was configured
     */
    public Weight weightOf(String property) {
        return weights.getOrDefault(property, Weight.D);
    }

    /**
     * Whether the results must be ordered by relevance: a search text is given and the sort either asks for
     * {@link #RELEVANCE} or is empty.
     *
     * @param sort
     *         the requested sort
     * @return true if executors must order by relevance
     */
    public boolean ranksByRelevance(Sort sort) {
        return isActive() && (sort.isUnsorted() || sort.getOrderFor(RELEVANCE) != null);
    }

    /**
     * Removes the {@link #RELEVANCE} order from a sort, leaving the orders that map to real properties.
     *
     * @param sort
     *         the requested sort
     * @return the sort without the relevance order
     */
    public static Sort withoutRelevance(Sort sort) {
        return Sort.by(sort.stream().filter(order -> !RELEVANCE.equals(order.getProperty())).toList());
    }
}
