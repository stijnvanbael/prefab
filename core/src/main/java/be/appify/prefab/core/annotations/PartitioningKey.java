package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate an event field or no-argument method to indicate that it determines the partitioning or ordering key.
 * This key decides which partition the event is sent to in a messaging system, and event order is typically
 * preserved within a partition.
 *
 * <p>Annotate a method when the routing key is derived from existing event properties rather than stored as a
 * serialized field. Partitioning-key methods must be invokable without arguments and must resolve to a
 * {@link String} value, either directly or through a single-value wrapper such as a reference type.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.SOURCE)
public @interface PartitioningKey {
}
