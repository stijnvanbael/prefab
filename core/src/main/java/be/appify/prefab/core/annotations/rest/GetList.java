package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotate the aggregate root class to expose an HTTP endpoint for retrieving a list of aggregate roots. Additionally,
/// add @Filter annotations on fields to enable filtering on those fields.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GetList {
    String method() default HttpMethod.GET;

    String path() default "";
}
