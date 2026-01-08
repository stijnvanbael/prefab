package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a type to expose an HTTP endpoint for deleting the aggregate root. Alternatively, annotate a method to
 * perform some logic before deleting, like publishing an event. A delete method should not take any parameters. Only
 * one delete method is allowed per aggregate root.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.SOURCE)
public @interface Delete {
    /**
     * The HTTP method to use for this endpoint. Default is DELETE.
     *
     * @return The HTTP method to use for this endpoint.
     */
    String method() default HttpMethod.DELETE;

    /**
     * The path to use for this endpoint. Default is "/{id}".
     *
     * @return The path to use for this endpoint.
     */
    String path() default "/{id}";

    /**
     * Security settings for this endpoint. Default is secured with no required authorities.
     *
     * @return Security settings for this endpoint.
     */
    Security security() default @Security;
}
