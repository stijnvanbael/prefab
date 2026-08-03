package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotate a class to be an aggregate root, a first-class entity in the domain model. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Aggregate {
    /**
     * A custom plural form for this aggregate root, used wherever Prefab generates a plural (REST paths, OpenAPI
     * summaries, generated test client method names, log messages). When left empty, the plural is derived
     * automatically via {@code org.javalite.common.Inflector.pluralize(...)}.
     *
     * @return the custom plural form, or an empty string to use the default Inflector-derived plural
     */
    String plural() default "";
}
