package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or type for JSONB storage in PostgreSQL.
 *
 * <p>When placed on a field, the field's value is serialized as a JSONB document and stored in a single column.
 * This works for value objects (records), lists of child entities, and any other complex type.</p>
 *
 * <p>When placed on a type (record), all occurrences of that type in aggregate roots and child entities
 * are automatically stored as JSONB documents.</p>
 *
 * <p>Fields inside a {@code @DbDocument} type can be annotated with {@link Indexed} to create indexes
 * that target specific fields within the JSONB document.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @DbDocument
 * public record Address(
 *     @Indexed String city,
 *     String street
 * ) {}
 *
 * @Aggregate
 * public record Customer(
 *     @Id Reference<Customer> id,
 *     String name,
 *     Address shippingAddress  // stored as JSONB in "shipping_address" column
 * ) {}
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DbDocument {
}
