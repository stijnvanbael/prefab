package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a static method to be an event handler. The method should have a single parameter of the desired event type.
 * The method should return the aggregate root that has been created, or an optional of the aggregate root.
 *
 * <p>Optionally, set {@link #value()} to an aggregate root class to merge this component's event handler into the
 * aggregate root's service. The specified class must be annotated with
 * {@link be.appify.prefab.core.annotations.Aggregate}; a compiler error is raised otherwise.
 *
 * <p>When used on a <strong>static</strong> method, the method must be public and static, returning the enclosing
 * component type (or {@code Optional} thereof). When used on an <strong>instance</strong> method, the enclosing
 * class must be annotated with Spring's {@code @Component}; the component bean is injected into the aggregate
 * root's service and the method is called directly on it.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface EventHandler {

    /**
     * The aggregate root class whose service this event handler should be merged into.
     * When set on a static method, the annotated method must be a public static method returning the enclosing
     * component type (or {@code Optional} thereof), and the specified class must be an aggregate root annotated with
     * {@link be.appify.prefab.core.annotations.Aggregate}.
     * When set on an instance method, the enclosing class must be annotated with Spring's {@code @Component};
     * the component is injected into the aggregate root's service and the method is called on it.
     *
     * @return the aggregate root class, or {@code void.class} if not merging (the default)
     */
    Class<?> value() default void.class;
}
