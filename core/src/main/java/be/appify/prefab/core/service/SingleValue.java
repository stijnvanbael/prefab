package be.appify.prefab.core.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as a single-value object, i.e. a type that wraps a single scalar value.
 * <p>
 * Types annotated with {@code @SingleValue} are treated as scalar values in the Prefab framework,
 * meaning they are serialized and stored as their underlying value rather than as nested objects.
 * </p>
 * <p>
 * For example, a {@code Reference} type annotated with {@code @SingleValue("id")} will be stored as
 * a plain {@code VARCHAR} in the database and serialized as a plain string in Avro/JSON.
 * </p>
 *
 * @see Reference
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SingleValue {
    /**
     * The name of the accessor method that returns the single value.
     *
     * @return the accessor method name, defaults to {@code "value"}
     */
    String value() default "value";
}
