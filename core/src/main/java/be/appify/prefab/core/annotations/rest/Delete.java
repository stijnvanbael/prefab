package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotate a method that should be exposed as an HTTP endpoint for deleting the aggregate root.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Delete {
    String method() default HttpMethod.DELETE;

    String path() default "/{id}";

    Security security() default @Security;
}
