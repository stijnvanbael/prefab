package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a static method to be an event handler. The method should have a single parameter of the desired event type.
 * The method should return the aggregate root that has been created, or an optional of the aggregate root.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface EventHandler {

}
