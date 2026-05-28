package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a field on the aggregate root to expose an HTTP endpoint for field-level autocompletion.
 * Generates a dedicated endpoint returning distinct matching values for the annotated field.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Autocomplete {
    /**
     * The path to use for this endpoint, relative to the aggregate root base path.
     * Default is the kebab-case version of the field name with "/autocomplete" appended.
     *
     * @return The path to use for this endpoint.
     */
    String path() default "";

    /**
     * Whether the autocomplete match should be case-insensitive. Default is false.
     *
     * @return true if the autocomplete should be case-insensitive.
     */
    boolean ignoreCase() default false;

    /**
     * Security settings for this endpoint. Default is secured with no required authorities.
     *
     * @return Security settings for this endpoint.
     */
    Security security() default @Security;
}
