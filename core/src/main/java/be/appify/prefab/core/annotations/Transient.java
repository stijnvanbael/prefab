package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an aggregate or value-object field as part of the in-memory domain model while excluding it from database
 * persistence.
 *
 * <p>Prefab keeps transient fields in generated REST request/response DTOs and event payloads, but omits them from
 * generated Flyway migrations and Spring Data JDBC persistence metadata. When a transient field appears inside a
 * {@link DbDocument}, it is also excluded from the stored JSONB document.</p>
 */
@org.springframework.data.annotation.Transient
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {
}
