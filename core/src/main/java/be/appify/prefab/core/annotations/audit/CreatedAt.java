package be.appify.prefab.core.annotations.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be populated with the timestamp of when the aggregate was first created.
 * <p>
 * Place this annotation on a field of type {@link java.time.Instant} in an aggregate. The annotation
 * processor will generate code to set this field to the current time on creation and never overwrite it
 * on subsequent updates.
 * </p>
 * <p>
 * For convenience, use {@link be.appify.prefab.core.audit.AuditInfo} to group all four audit fields together.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreatedAt {
}
