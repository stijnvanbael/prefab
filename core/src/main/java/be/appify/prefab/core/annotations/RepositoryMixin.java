package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotate an interface to be a repository mixin, providing additional methods to repositories. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepositoryMixin {
    /**
     * The aggregate type to add the repository mixin for.
     *
     * @return The aggregate type to add the repository mixin for.
     */
    Class<?> value();
}
