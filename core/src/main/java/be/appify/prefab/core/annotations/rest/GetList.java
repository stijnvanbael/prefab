package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate the aggregate root class to expose an HTTP endpoint for retrieving a list of aggregate roots. Additionally,
 * add <code>@Filter</code> annotations on fields to enable filtering on those fields.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GetList {
    /**
     * The HTTP method to use for this endpoint. Default is GET.
     *
     * @return The HTTP method to use for this endpoint.
     */
    String method() default HttpMethod.GET;

    /**
     * The path to use for this endpoint. Default is empty, meaning the base path of the aggregate root.
     *
     * @return The path to use for this endpoint.
     */
    String path() default "";

    /**
     * Security settings for this endpoint. Default is secured with no required authorities.
     *
     * @return Security settings for this endpoint.
     */
    Security security() default @Security;
}
