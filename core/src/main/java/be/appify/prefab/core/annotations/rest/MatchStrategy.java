package be.appify.prefab.core.annotations.rest;

/**
 * Controls how the autocomplete query term is compared against field values.
 *
 * @see Autocomplete#matchStrategy()
 */
public enum MatchStrategy {
    /**
     * Byte-for-byte, case-sensitive comparison.
     * <p>No column wrapping is applied.</p>
     */
    EXACT,

    /**
     * Case-insensitive comparison.
     * <p>Wraps the column in {@code LOWER()} for JDBC;
     * appends {@code '$options':'i'} to the regex stage for MongoDB.</p>
     */
    IGNORE_CASE,

    /**
     * Fuzzy similarity match.
     * <p>For JDBC, requires the {@code pg_trgm} PostgreSQL extension and uses
     * {@code similarity(col, :query) > 0.3}. For MongoDB, falls back to a
     * case-insensitive regex as the closest approximation — document this limitation
     * to users.</p>
     */
    FUZZY,

    /**
     * Full-text type-ahead match: every word of the query must occur in the value, in any order and position, and the
     * last word may be a prefix, so "spring boo" finds "Spring Boot Handbook". {@link ScanMode} has no effect.
     * <p>For JDBC, uses a PostgreSQL {@code to_tsvector} / {@code to_tsquery} match over the same expression as the
     * generated GIN index. The query is reduced to letters and digits, so it can never break the tsquery. For MongoDB,
     * {@code $text} cannot match prefixes, so it falls back to a case-insensitive regex that requires the whole query
     * to start at a word boundary of the value — words are not matched independently or in any order.</p>
     */
    FULL_TEXT
}

