package be.appify.prefab.core.annotations.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be automatically set to the current timestamp when the aggregate is created.
 * The field value is never overwritten on subsequent updates.
 * <p>
 * Typically applied to a field of type {@link java.time.Instant}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface CreatedAt {
}
