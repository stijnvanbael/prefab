package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotate one or more fields to enable filtering on those fields in the HTTP endpoint created by @GetList.
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Filters.class)
public @interface Filter {

    /// The operator to use for filtering. Default is CONTAINS.
    Operator operator() default Operator.CONTAINS;

    /// Whether the filter should ignore case. Default is true.
    boolean ignoreCase() default true;

    enum Operator {
        EQUAL,
        CONTAINS,
        MATCHES_REGEX,
        STARTS_WITH,
        ENDS_WITH
    }
}
