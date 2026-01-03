package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotate a field with multiple filters for REST endpoints. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Filters {
    /**
     * The filters to apply to the field.
     * @return The filters to apply to the field.
     */
    Filter[] value();
}
