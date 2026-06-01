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
    FUZZY
}

