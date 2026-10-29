package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;

/**
 * Marks a {@link be.appify.prefab.core.versioning.Migration} as the migration from one major version of an event (or
 * {@link Versioned} component) to the next. The annotated class is registered as a Spring bean, so it can use
 * dependency injection.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface EventMigration {
    /**
     * The major version the migration reads, for example {@code "1"}.
     *
     * @return the major version of the old shape
     */
    String from();

    /**
     * The major version the migration produces, for example {@code "2"}.
     *
     * @return the major version of the new shape
     */
    String to();

    /**
     * The fully-qualified or simple name the event type had in the old version, when the type was renamed.
     *
     * @return the previous type name, empty if the type was not renamed
     */
    String fromType() default "";
}
