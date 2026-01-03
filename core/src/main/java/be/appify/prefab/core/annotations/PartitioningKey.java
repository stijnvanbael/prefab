package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a field to indicate that it is used to determine the partitioning key of an event. This key determines
 * which partition the event is sent to in a messaging system. Typically, event order is guaranteed within a
 * partition.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.SOURCE)
public @interface PartitioningKey {
}
