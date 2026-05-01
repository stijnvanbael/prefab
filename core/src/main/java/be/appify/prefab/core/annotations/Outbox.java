package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an aggregate to use the transactional outbox pattern for domain event publishing.
 * When enabled, events are stored in the same transaction as the aggregate state change,
 * then relayed to the broker by a background process.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Outbox {

    /** Whether the outbox pattern is enabled for this aggregate. Defaults to {@code true}. */
    boolean enabled() default true;
}
