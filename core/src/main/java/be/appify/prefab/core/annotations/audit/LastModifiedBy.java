package be.appify.prefab.core.annotations.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be automatically updated to the identity of the authenticated principal on every write operation
 * (create and update).
 * <p>
 * Typically applied to a field of type {@link String}.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface LastModifiedBy {
}
