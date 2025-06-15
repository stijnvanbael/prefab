package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotate a method that should be exposed as an HTTP endpoint for updating the aggregate root.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Update {
    String method() default HttpMethod.PUT;

    String path() default "/{id}";
}
