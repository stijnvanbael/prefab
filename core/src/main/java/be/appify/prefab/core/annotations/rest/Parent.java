package be.appify.prefab.core.annotations.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Annotate a Reference field with @Parent to indicate that it is the parent of this entity.
 * Parent IDs need to be included in the request path for every REST call of this entity.
 */
@Target(ElementType.FIELD)
@Retention(SOURCE)
public @interface Parent {
}
