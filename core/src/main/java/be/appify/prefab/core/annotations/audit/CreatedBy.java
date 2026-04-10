package be.appify.prefab.core.annotations.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be populated with the identity of the principal who created the aggregate.
 * <p>
 * Place this annotation on a {@link String} field in an aggregate. The annotation processor will
 * generate code to set this field to the value returned by
 * {@link be.appify.prefab.core.audit.AuditContextProvider#currentUserId()} on creation and never
 * overwrite it on subsequent updates.
 * </p>
 * <p>
 * For convenience, use {@link AuditInfo} to group all four audit fields together.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreatedBy {
}
