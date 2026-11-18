package be.appify.prefab.spike.usecase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks one application operation for generation of an HTTP adapter. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface UseCase {
    /** Returns the HTTP method for the generated adapter. */
    String method();

    /** Returns the absolute route path for the generated adapter. */
    String path();
}
