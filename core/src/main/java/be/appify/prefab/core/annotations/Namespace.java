package be.appify.prefab.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the Avro namespace to use when generating schemas for a record type.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Namespace {

    /**
     * Avro namespace for the annotated type.
     *
     * @return the Avro namespace
     */
    String value();
}

