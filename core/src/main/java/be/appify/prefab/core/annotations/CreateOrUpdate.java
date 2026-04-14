package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a static method to be a create-or-update event handler. The method should have two parameters: an
 * {@link java.util.Optional} of the aggregate root type (the existing aggregate, or empty if not found) and the event
 * type. The method should return the aggregate root that has been created or updated. The framework will look up the
 * existing aggregate by the specified event property and always save the returned aggregate.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface CreateOrUpdate {
    /**
     * The name of the property on the event that contains the reference or identifier used to look up the aggregate root.
     *
     * @return The name of the property on the event that contains the reference to the aggregate root.
     */
    String property() default "";
}
