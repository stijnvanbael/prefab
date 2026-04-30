package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an aggregate class, a {@code @Create} static factory method, or an {@code @Update} void method
 * as using the async-commit (listen-to-self) pattern.
 *
 * <p>When placed on an aggregate class, all {@code @Create} and {@code @Update} methods on that class
 * use the async-commit strategy. When placed on an individual method, only that method is async-commit.
 *
 * <p>Async-commit semantics:
 * <ul>
 *   <li>A {@code @Create} static factory method must return the event type. The processor generates a REST
 *       endpoint that calls the method, publishes the returned event, and returns {@code 202 Accepted}.
 *       No aggregate is persisted at this point.</li>
 *   <li>An {@code @Update} void method calls {@code publish()} internally. The processor generates a REST
 *       endpoint that loads the aggregate, calls the method, and returns {@code 202 Accepted} without saving.
 *       </li>
 *   <li>{@code @EventHandler} methods on an {@code @AsyncCommit} aggregate automatically receive a
 *       deduplication guard in the generated broker consumer. The deduplication key is taken from the
 *       {@code @EventId}-annotated field on the event record.</li>
 * </ul>
 *
 * <p>Non-{@code @AsyncCommit} aggregates are completely unaffected.
 */
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface AsyncCommit {
}

