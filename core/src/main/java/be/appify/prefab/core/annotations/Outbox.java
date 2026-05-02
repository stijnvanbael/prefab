package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controls the transactional outbox behaviour for an aggregate.
 * <p>
 * By default, Prefab stores domain events in the outbox table/collection in the same transaction
 * as the aggregate state change, then relays them to the broker via the background
 * {@code OutboxRelayService}. This ensures at-least-once delivery even if the application crashes
 * between the persist and the publish step.
 * </p>
 * <p>
 * Annotate an aggregate with {@code @Outbox(enabled = false)} to bypass the outbox and publish
 * events directly via the Spring {@code ApplicationEventPublisher}:
 * </p>
 * <pre>
 * &#64;Aggregate
 * &#64;Outbox(enabled = false)
 * public record Notification(...) { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Outbox {

    /** When {@code false}, events are published directly, bypassing the outbox. Defaults to {@code true}. */
    boolean enabled() default true;
}
