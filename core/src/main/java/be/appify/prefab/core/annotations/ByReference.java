package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate the method of an aggregate root to have it process domain events. The method should have a single parameter
 * of the desired event type. The aggregate root will be fetched using the specified property of type
 * <code>Reference&lt;A></code> where <code>A</code> is the aggregate root type.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface ByReference {
    /**
     * The name of the property on the event that contains the reference to the aggregate root.
     *
     * @return The name of the property on the event that contains the reference to the aggregate root.
     */
    String property() default "";
}
