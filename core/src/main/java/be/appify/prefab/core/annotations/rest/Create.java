package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotate the constructor that should be exposed as an HTTP endpoint for creating the aggregate root. */
@Target(ElementType.CONSTRUCTOR)
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
