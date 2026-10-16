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
         * supported per aggregate and {@link #ignoreCase()} has no effect. Backed by PostgreSQL text search (a GIN
         * index is generated) and MongoDB text indexes.
         */
        FULL_TEXT
    }
}
