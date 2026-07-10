package be.appify.prefab.core.annotations;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public no-argument method of an aggregate root or value object as a synthetic, read-only field in REST
 * responses.
 *
 * <p>The method's return value is exposed under the method's name:</p>
 * <ul>
 *   <li>On an aggregate root (or polymorphic subtype), the annotation processor adds a component with the method's
 *       name and return type to the generated response record, populated from the method in the record's
 *       {@code from(...)} factory method.</li>
 *   <li>On a value object, the method result is serialized as a read-only Jackson property wherever the value object
 *       is embedded in a response.</li>
 * </ul>
 *
 * <p>The synthetic field is never part of request records, is ignored when deserializing request bodies, and is not
 * mapped to a database column or stored in {@link DbDocument} JSONB documents.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Aggregate
 * public record Order(
 *     @Id Reference<Order> id,
 *     @Version long version,
 *     List<OrderLine> lines
 * ) {
 *     @Computed
 *     public BigDecimal total() {
 *         return lines.stream().map(OrderLine::price).reduce(BigDecimal.ZERO, BigDecimal::add);
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonProperty(access = JsonProperty.Access.READ_ONLY)
public @interface Computed {
}
