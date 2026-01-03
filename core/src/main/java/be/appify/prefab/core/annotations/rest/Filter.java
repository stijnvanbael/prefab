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
        ENDS_WITH
    }
}
