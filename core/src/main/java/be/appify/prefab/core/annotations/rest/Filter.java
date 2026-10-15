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
        LESS_THAN_OR_EQUAL
    }
}
