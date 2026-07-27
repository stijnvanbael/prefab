package be.appify.prefab.core.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one AVSC schema file consumed by {@link Avsc} and, optionally, which generated property should be used as
 * the event partitioning key.
 */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface AvscFile {

    /**
     * The classpath-relative path to the AVSC schema file.
     *
     * @return the schema path
     */
    String path();

    /**
     * The generated record property to use as the partitioning key extractor. Leave blank to use no partitioning key
     * for this AVSC-generated event.
     *
     * @return the generated record property used as partitioning key
     */
    String keyProperty() default "";
}
