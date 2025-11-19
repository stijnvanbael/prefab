package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Annotate a Binary field to expose a download endpoint for that field.
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface Download {
}
