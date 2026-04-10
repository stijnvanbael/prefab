package be.appify.prefab.core.annotations.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be updated with the current user identity on every write operation (create and update).
 * <p>
 * Place this annotation on a {@link String} field in an aggregate. The annotation processor will
 * generate code to set this field to the value returned by
 * {@link be.appify.prefab.core.audit.AuditContextProvider#currentUserId()} on both creation and every
 * subsequent update.
 * </p>
 * <p>
 * For convenience, use {@link be.appify.prefab.core.audit.AuditInfo} to group all four audit fields together.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LastModifiedBy {
}
