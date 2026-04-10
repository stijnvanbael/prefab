package be.appify.prefab.core.annotations.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be automatically updated to the current timestamp on every write operation (create and update).
 * <p>
 * Typically applied to a field of type {@link java.time.Instant}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface LastModifiedAt {
}
