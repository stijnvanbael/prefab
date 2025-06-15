package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotate the aggregate root class to expose an HTTP endpoint for retrieving an aggregate root by its ID.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GetById {
    String method() default HttpMethod.GET;

    String path() default "/{id}";
}
