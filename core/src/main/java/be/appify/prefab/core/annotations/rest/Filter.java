package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate one or more fields to enable filtering on those fields in the HTTP endpoint created by
 * <code>@GetList</code>.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Filters.class)
public @interface Filter {

    /**
     * The operator to use for filtering. Default is CONTAINS.
     * @return The operator to use for filtering.
     */
    Operator operator() default Operator.CONTAINS;

    /**
     * Whether the filter should ignore case. Default is true.
     * @return Whether the filter should ignore case.
     */
    boolean ignoreCase() default true;

    /**
     * The name of the request parameter. Only used by {@link Operator#FULL_TEXT}, where several fields can share
     * the same parameter so that one search text is matched against all of them. Defaults to the field name.
     * @return The name of the request parameter.
     */
    String parameter() default "";

    /**
     * Whether a {@link Operator#FULL_TEXT} search also tolerates typos, by additionally matching words that are
     * similar to the search text (PostgreSQL pg_trgm word similarity). Default is false, which keeps exact full-text
     * matching. MongoDB has no fuzzy text search: the attribute is ignored there and the search stays exact.      All
          * fields sharing a parameter must use the same fuzzy settings, otherwise compilation fails.
     * @return Whether to match fuzzily.
     */
    boolean fuzzy() default false;

    /**
     * The minimum word similarity, between 0 (exclusive) and 1, for a fuzzy {@link Operator#FULL_TEXT} match. Lower
     * values tolerate more typos but match more rows. Only used when {@link #fuzzy()} is true.
     * @return The similarity threshold.
     */
    double similarityThreshold() default 0.3;

    /**
     * Whether a {@link Operator#FULL_TEXT} search works as a type-ahead search: every word of a plain search text
     * (without double quotes, excluded words or <code>OR</code>) also matches as a prefix, in any order, so that
     * "spr bo g" finds "Spring Boot guide" while the user is still typing. A single word of two or more letters
     * also matches the initials of the words of a field, so "sbg" finds "Spring Boot guide". Default is true; set it
     * to false to match whole words only. A search text with web search operators keeps web search semantics and only
     * its last word is a prefix, and only when the text does not end in whitespace or punctuation, the word is not
     * excluded with <code>-</code>, not inside an open double quote and not <code>OR</code>. Prefixes only consist of
     * letters and digits, so the search text can never break the query. On PostgreSQL prefixes are matched with
     * <code>to_tsquery</code> and <code>:*</code>, initials with a generated btree index. On MongoDB,
     * <code>$text</code> only matches whole words, so prefixes and initials are case-insensitive regular expressions
     * on the searched fields and the result is not ranked by relevance unless the text has web search words. Every
     * word must match some searched field. All fields sharing a parameter must use the same setting, otherwise
     * compilation fails.
     * @return Whether the words are matched as prefix.
     */
    boolean typeahead() default true;

    /**
     * The weight of this field in the relevance score of a {@link Operator#FULL_TEXT} search, so that for example a
     * title counts for more than a body. On PostgreSQL this is the <code>setweight</code> label of the field in the
     * generated index and in the ranking, on MongoDB it is the weight of the field in the text index. Default is
     * {@link Weight#D}, the lowest weight. Only used by {@link Operator#FULL_TEXT}.
     * @return The weight of the field.
     */
    Weight weight() default Weight.D;

    /**
     * The language of a {@link Operator#FULL_TEXT} search, so that stemming and stop words apply, for example
     * <code>english</code> or <code>dutch</code>. Use the name of a built-in PostgreSQL text search configuration;
     * an unknown name fails compilation. Empty (the default) uses the application-wide default of the Spring
     * property <code>prefab.full-text.language</code>, which is <code>simple</code>, without stemming or stop words,
     * when absent. On PostgreSQL the language is used in the query and in the generated GIN index, and changing it
     * generates a migration that rebuilds the index. On MongoDB it is the language of the text index and the
     * <code>$text</code> query. All fields sharing a parameter must use the same language, otherwise compilation
     * fails.
     * @return The language, or empty for the default.
     */
    String language() default "";

    /** The weight of a field in the relevance score of a full-text search, from most to least important. */
    enum Weight {
        /** Highest weight. */
        A(10f),
        /** High weight. */
        B(4f),
        /** Medium weight. */
        C(2f),
        /** Default and lowest weight. */
        D(1f);

        private final float textIndexWeight;

        Weight(float textIndexWeight) {
            this.textIndexWeight = textIndexWeight;
        }

        /**
         * The weight of the field in a MongoDB text index, proportional to the default PostgreSQL ranking weights.
         *
         * @return the text index weight
         */
        public float textIndexWeight() {
            return textIndexWeight;
        }
    }

    /** The supported filter operators. */
    enum Operator {
        /** Equal operator. */
        EQUAL,
        /** Contains operator. */
        CONTAINS,
        /** Matches regex operator. */
        MATCHES_REGEX,
        /** Starts with operator. */
        STARTS_WITH,
        /** Ends with operator. */
        ENDS_WITH,
        /**
         * Greater than operator. Exposed as request parameter <code>&lt;field&gt;GreaterThan</code>. Only
         * supported on numeric and <code>java.time</code> fields; {@link #ignoreCase()} has no effect.
         */
        GREATER_THAN,
        /**
         * Greater than or equal operator. Exposed as request parameter <code>&lt;field&gt;GreaterThanOrEqual</code>.
         * Only supported on numeric and <code>java.time</code> fields; {@link #ignoreCase()} has no effect.
         */
        GREATER_THAN_OR_EQUAL,
        /**
         * Less than operator. Exposed as request parameter <code>&lt;field&gt;LessThan</code>. Only supported on
         * numeric and <code>java.time</code> fields; {@link #ignoreCase()} has no effect.
         */
        LESS_THAN,
        /**
         * Less than or equal operator. Exposed as request parameter <code>&lt;field&gt;LessThanOrEqual</code>.
         * Only supported on numeric and <code>java.time</code> fields; {@link #ignoreCase()} has no effect.
         */
        LESS_THAN_OR_EQUAL,
        /**
         * Full-text search operator. Only supported on <code>String</code> fields. All fields of an aggregate that
         * use this operator share a single request parameter (see {@link Filter#parameter()}, default is the name of the
         * field) holding the search text; a row matches when the text matches any of those fields. The search text
         * supports web search syntax: multiple words must all match, <code>OR</code> matches either word, a leading
         * <code>-</code> excludes a word and double quotes match a phrase. Only one full-text request parameter is
         * supported per aggregate and {@link #ignoreCase()} has no effect. Results can be ordered by relevance with the
         * sort property <code>relevance</code>, which is also the default order when a search text is given and no
         * sort was requested; see {@link #weight()} for weighting fields. Backed by PostgreSQL text search (a GIN
         * index is generated) and MongoDB text indexes.
         */
        FULL_TEXT
    }
}
