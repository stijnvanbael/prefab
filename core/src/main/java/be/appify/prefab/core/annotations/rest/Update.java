package be.appify.prefab.core.annotations.rest;

import be.appify.prefab.core.annotations.Exposure;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method exposed as an HTTP endpoint for updating the aggregate root.
 *
 * <p>Return an updated aggregate instance instead of mutating the existing instance. The legacy
 * {@code void} update form is deprecated and scheduled for removal in the next major version;
 * the annotation processor warns when it is used. Mark existing legacy methods with
 * {@link Deprecated @Deprecated} and document their aggregate-returning replacement.
 *
 * <p>For async commits, publish the event and return the current aggregate; the generated service
 * does not persist the return value.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Update {
    /**
     * Overrides the aggregate's default HTTP exposure for this operation.
     *
     * @return the exposure override, or {@link Exposure#INHERIT}
     */
    Exposure exposure() default Exposure.INHERIT;

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
