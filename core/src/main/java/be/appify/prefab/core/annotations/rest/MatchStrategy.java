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
     * Full-text type-ahead match: every word of the query is the start of a word in the value, in any order, so
     * "spr bo g" finds "Spring Boot guide". A single word of two or more letters also matches the initials of the
     * words of the value, so "sbg" finds "Spring Boot guide". {@link ScanMode} has no effect.
     * <p>For JDBC, uses a PostgreSQL {@code to_tsvector} / {@code to_tsquery} match over the same expression as the
     * generated GIN index, and the initials of the value as generated btree index. The query is reduced to letters and
     * digits, so it can never break the tsquery. For MongoDB, {@code $text} cannot match prefixes, so it uses a
     * case-insensitive regex with the same semantics.</p>
     */
    FULL_TEXT,

    /**
     * {@link #FULL_TEXT}, and additionally tolerates typos: a value also matches when every word of the query is
     * similar to a word of the value or the start of one, so "sprint boo" finds "Spring Boot" but "spring post" does
     * not.
     * <p>For JDBC, requires the {@code pg_trgm} PostgreSQL extension and uses {@code word_similarity}. For MongoDB
     * there is no fuzzy support, so it behaves like {@link #FULL_TEXT}.</p>
     */
    FULL_TEXT_FUZZY
}