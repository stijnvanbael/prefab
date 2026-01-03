package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Annotate REST endpoints to define security settings. */
@Retention(RetentionPolicy.SOURCE)
public @interface Security {
    /**
     * Whether security is enabled for the endpoint. Default is true.
     *
     * @return true if security is enabled, false otherwise.
     */
    boolean enabled() default true;

    /**
     * The authority required to access the endpoint. Default is no authority required.
     *
     * @return The authority required to access the endpoint.
     */
    String authority() default "";
}
