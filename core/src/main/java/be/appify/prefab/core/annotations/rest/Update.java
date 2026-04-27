package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotate a method that should be exposed as an HTTP endpoint for updating the aggregate root. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Update {
    /**
     * The HTTP method to use for this endpoint. Default is PUT.
     *
     * @return The HTTP method to use for this endpoint.
     */
    String method() default HttpMethod.PUT;

    /**
     * The path suffix appended after "/{id}" for this endpoint. Default is "" (empty), resulting in "/{id}".
     *
     * @return The path suffix to append after "/{id}".
     */
    String path() default "";

    /**
     * Security settings for this endpoint. Default is secured with no required authorities.
     *
     * @return Security settings for this endpoint.
     */
    Security security() default @Security;
}
