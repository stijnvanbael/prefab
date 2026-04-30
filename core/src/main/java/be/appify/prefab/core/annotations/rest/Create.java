package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate the constructor (or, for async-commit aggregates, a public static factory method) that
 * should be exposed as an HTTP endpoint for creating the aggregate root.
 *
 * <p>On a synchronous aggregate the target must be a constructor. On an {@code @AsyncCommit} aggregate
 * the target must be a {@code public static} method returning the event type; the processor will call
 * it, publish the returned event, and return {@code 202 Accepted}.
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Create {
    /**
     * The HTTP method to use for this endpoint. Default is POST.
     *
     * @return The HTTP method to use for this endpoint.
     */
    String method() default HttpMethod.POST;

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
