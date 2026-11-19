package be.appify.prefab.core.annotations.rest;

import be.appify.prefab.core.annotations.Exposure;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotate the aggregate root class to expose an HTTP endpoint for retrieving an aggregate root by its ID. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GetById {
    /**
     * Overrides the aggregate's default HTTP exposure for this operation.
     *
     * @return the exposure override, or {@link Exposure#INHERIT}
     */
    Exposure exposure() default Exposure.INHERIT;

    /**
     * The HTTP method to use for this endpoint. Default is GET.
     * @return The HTTP method to use for this endpoint.
     */
    String method() default HttpMethod.GET;

    /**
     * The path to use for this endpoint. Default is "/{id}".
     * @return The path to use for this endpoint.
     */
    String path() default "/{id}";

    /**
     * Security settings for this endpoint. Default is secured with no required authorities.
     * @return Security settings for this endpoint.
     */
    Security security() default @Security;
}
