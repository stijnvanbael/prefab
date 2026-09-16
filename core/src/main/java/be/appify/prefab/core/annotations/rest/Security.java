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
     * <p>Use this for full Spring Security authorities such as {@code ROLE_ADMIN} or
     * {@code SCOPE_messages.read}. Mutually exclusive with {@link #role()}.
     *
     * @return The authority required to access the endpoint.
     */
    String authority() default "";

    /**
     * The role required to access the endpoint. Default is no role required.
     *
     * <p>Use the unprefixed Spring Security role name such as {@code ADMIN}; generated source
     * applies role semantics via {@code hasRole("ADMIN")}. Mutually exclusive with
     * {@link #authority()}.
     *
     * @return The required role name.
     */
    String role() default "";
}
